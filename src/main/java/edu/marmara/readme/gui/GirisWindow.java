package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Python giris_ekle_guncelle_window.py'nin Java/JavaFX portu. */
public class GirisWindow {

    private final Path girisJsonYolu;
    private GirisBolumu data;
    private final ObservableList<String> icindekilerModel = FXCollections.observableArrayList();
    private final UndoManager<String> undoManager = new UndoManager<>();
    private Stage stage;
    private TextField aramaAlani;
    private Label sayiLabel;
    private ListView<String> listView;

    public GirisWindow(Path jsonDeposu) {
        this.girisJsonYolu = jsonDeposu.resolve(ReadmeGenerator.GIRIS_JSON);
    }

    public void show() {
        data = JsonUtil.read(girisJsonYolu, GirisBolumu.class);
        if (data == null) {
            data = new GirisBolumu();
            data.baslik = Sabitler.VARSAYILAN_GIRIS_BASLIK;
            data.aciklama = Sabitler.VARSAYILAN_GIRIS_ACIKLAMA;
            jsonKaydet();
        }

        stage = new Stage();
        stage.setTitle("Giriş Güncelle");
        stage.initModality(Modality.APPLICATION_MODAL);

        Button baslikBtn = new Button();
        baslikBtn.setMaxWidth(Double.MAX_VALUE);
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Başlık", "Giriş başlığı:", data.baslik).ifPresent(yeni -> {
            data.baslik = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
            Toast.show(stage, "Başlık güncellendi.", Toast.Tip.BASARI);
        }));

        Button aciklamaBtn = new Button();
        aciklamaBtn.setMaxWidth(Double.MAX_VALUE);
        aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
        aciklamaBtn.setOnAction(e -> Dialogs.multilineInput("Açıklama", "Giriş açıklaması:", data.aciklama).ifPresent(yeni -> {
            data.aciklama = yeni;
            jsonKaydet();
            aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
            Toast.show(stage, "Açıklama güncellendi.", Toast.Tip.BASARI);
        }));

        Button geriBildirimBtn = metinButonu("Geri bildirim paragrafı",
                "\"Geri Bildirimde Bulunun\" başlığının altındaki paragraf (form linkleri konfigürasyondan gelir):",
                () -> data.geri_bildirim_aciklamasi, s -> data.geri_bildirim_aciklamasi = s);
        Button katkidaBulununBtn = metinButonu("Katkıda Bulunun bölümü",
                "\"## 🤝 Katkıda Bulunun\" bölümünün tamamı, Video Rehberler dahil (markdown):",
                () -> data.katkida_bulunun, s -> data.katkida_bulunun = s);
        Button sayacBtn = metinButonu("Görüntülenme sayacı",
                "Sayaç HTML'i. Adresteki kullanıcı adını değiştirirsen sayım sıfırdan başlar:",
                () -> data.sayac, s -> data.sayac = s);

        icindekilerModel.setAll(data.icindekiler);
        listView = new ListView<>(icindekilerModel);
        DragReorder.enable(listView, yeniSira -> {
            data.icindekiler = new ArrayList<>(yeniSira);
            jsonKaydet();
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String secili = listView.getSelectionModel().getSelectedItem();
                if (secili != null) {
                    icerikDuzenle(secili);
                }
            }
        });

        aramaAlani = new TextField();
        aramaAlani.setPromptText("İçindekiler ara...");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> uygulaFiltre(yeni));
        sayiLabel = new Label();

        Button ekleBtn = new Button("➕ İçindekiler Maddesi Ekle");
        ekleBtn.setOnAction(e -> icerikEkle());

        Button silBtn = new Button("Seçili Maddeyi Sil");
        silBtn.setOnAction(e -> {
            String secili = listView.getSelectionModel().getSelectedItem();
            if (secili != null && Dialogs.confirm("Onay", "Bu içerik maddesini silmek istiyor musun?")) {
                int index = data.icindekiler.indexOf(secili);
                undoManager.pushDeleted(index, secili);
                icindekilerModel.remove(secili);
                data.icindekiler.remove(secili);
                jsonKaydet();
                Toast.show(stage, "Madde silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
            }
        });

        VBox root = new VBox(8, baslikBtn, aciklamaBtn, new HBox(8, geriBildirimBtn, katkidaBulununBtn, sayacBtn),
                new HBox(8, ekleBtn, silBtn, aramaAlani), sayiLabel,
                new Label("İçindekiler (çift tıkla: düzenle, sürükle: sırala)"), listView);
        root.setPadding(new Insets(10));
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root, 560, 540);
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                undoSil();
            }
        });
        stage.setScene(scene);
        guncelleSayi();
        stage.showAndWait();
    }

    private void guncelleSayi() {
        sayiLabel.setText("Toplam " + data.icindekiler.size() + " madde");
    }

    private void uygulaFiltre(String sorgu) {
        if (sorgu == null || sorgu.isEmpty()) {
            icindekilerModel.setAll(data.icindekiler);
            guncelleSayi();
            return;
        }
        String q = sorgu.replace("İ", "i").toLowerCase();
        List<String> filtreli = data.icindekiler.stream()
                .filter(s -> s.replace("İ", "i").toLowerCase().contains(q)).toList();
        icindekilerModel.setAll(filtreli);
        sayiLabel.setText(filtreli.size() + " madde bulundu");
    }

    private void undoSil() {
        var silinen = undoManager.popDeleted();
        if (silinen == null) {
            return;
        }
        int index = Math.min(silinen.index(), data.icindekiler.size());
        data.icindekiler.add(index, silinen.item());
        icindekilerModel.setAll(data.icindekiler);
        jsonKaydet();
        guncelleSayi();
        Toast.show(stage, "Madde geri alındı.", Toast.Tip.BASARI);
    }

    private void icerikEkle() {
        Dialogs.textInput("İçerik Başlığı", "Görünecek başlık:", "").ifPresent(baslik ->
                Dialogs.textInput("İçerik Çapası", "README.md çapası (örn: #-dersler):", "").ifPresent(capa -> {
                    if (!baslik.isBlank()) {
                        String madde = "[" + baslik + "](" + capa + ")";
                        icindekilerModel.add(madde);
                        data.icindekiler.add(madde);
                        jsonKaydet();
                        guncelleSayi();
                        Toast.show(stage, "Madde eklendi.", Toast.Tip.BASARI);
                    }
                }));
    }

    private void icerikDuzenle(String eski) {
        Dialogs.textInput("İçerik Düzenle", "Madde (markdown link formatında):", eski).ifPresent(yeni -> {
            int idx = icindekilerModel.indexOf(eski);
            icindekilerModel.set(idx, yeni);
            data.icindekiler.set(idx, yeni);
            jsonKaydet();
            Toast.show(stage, "Madde güncellendi.", Toast.Tip.BASARI);
        });
    }

    private void jsonKaydet() {
        JsonUtil.write(girisJsonYolu, data);
    }

    private Button metinButonu(String ad, String aciklama, Supplier<String> al, Consumer<String> yaz) {
        Button b = new Button(ad);
        b.setOnAction(e -> Dialogs.multilineInput(ad, aciklama, al.get()).ifPresent(yeni -> {
            if (!yeni.equals(al.get())) {
                yaz.accept(yeni);
                jsonKaydet();
                Toast.show(stage, ad + " güncellendi.", Toast.Tip.BASARI);
            }
        }));
        return b;
    }
}
