package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocaGrubu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
import edu.marmara.readme.engine.util.TurkceSira;
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
import java.util.Comparator;
import java.util.List;

/**
 * Hocaları ana README'deki sırayla (grup, sonra unvansız ada göre Türkçe alfabe) listeler;
 * ekleme, düzenleme, silme (Ctrl+Z ile geri alma), grup açıklamaları ve kırık link kontrolü.
 */
public class HocaEkleGuncelleWindow {

    private final Path hocalarJsonYolu;
    private final Path derslerJsonYolu;

    private HocalarBolumu data;
    private final ObservableList<Hoca> hocalarListModel = FXCollections.observableArrayList();
    private final UndoManager<Hoca> undoManager = new UndoManager<>();

    private ListView<Hoca> listView;
    private Label sayiLabel;
    private TextField aramaAlani;
    private Stage stage;

    public HocaEkleGuncelleWindow(Path jsonDeposu) {
        this.hocalarJsonYolu = jsonDeposu.resolve(ReadmeGenerator.HOCALAR_JSON);
        this.derslerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DERSLER_JSON);
    }

    public void show() {
        data = JsonUtil.read(hocalarJsonYolu, HocalarBolumu.class);
        if (data == null) {
            Dialogs.error("Hata", "hocalar.json bulunamadı. JSON klasörünü kontrol edin.");
            return;
        }

        stage = new Stage();
        stage.setTitle("Hocaları Ekle/Güncelle");
        stage.initModality(Modality.APPLICATION_MODAL);

        Button bolumAdiBtn = new Button("Hocalar başlığı");
        bolumAdiBtn.setOnAction(e -> Dialogs.textInput("Hocalar başlığı", "Başlık:", data.bolum_adi).ifPresent(yeni -> {
            data.bolum_adi = yeni;
            jsonKaydet();
        }));
        Button aciklamaBtn = new Button("Hocalar açıklaması");
        aciklamaBtn.setTooltip(new Tooltip(MetinIslemleri.kisaltMetin(data.bolum_aciklamasi)));
        aciklamaBtn.setOnAction(e -> Dialogs.multilineInput("Hocalar açıklaması", "Açıklama:", data.bolum_aciklamasi)
                .ifPresent(yeni -> {
                    data.bolum_aciklamasi = yeni;
                    jsonKaydet();
                    aciklamaBtn.setTooltip(new Tooltip(MetinIslemleri.kisaltMetin(yeni)));
                }));
        Button gruplarBtn = new Button("Grupları Düzenle");
        gruplarBtn.setOnAction(e -> gruplariDuzenle());

        Button ekleBtn = new Button("➕ Hoca Ekle");
        ekleBtn.setOnAction(e -> hocaDuzenlemeAc(null));
        Button linkKontrolBtn = new Button("🔗 Kırık Bağlantı Tetkiki");
        linkKontrolBtn.setOnAction(e -> new LinkKontrolWindow(data.hocalar.stream()
                .filter(h -> h.link != null && !h.link.isBlank())
                .map(h -> new LinkKontrolWindow.SahipLink(h.ad, h.link)).toList(), "Hoca Linkleri Kontrolü").show());

        aramaAlani = new TextField();
        aramaAlani.setPromptText("Hoca ara...");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> hocalariYenile());
        HBox.setHgrow(aramaAlani, Priority.ALWAYS);

        sayiLabel = new Label();
        listView = new ListView<>(hocalarListModel);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Hoca h, boolean empty) {
                super.updateItem(h, empty);
                setText(empty || h == null ? null
                        : h.grup + "  ›  " + h.emoji() + " " + h.ad + "   [" + h.kisaltma + "]");
            }
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                hocaDuzenlemeAc(listView.getSelectionModel().getSelectedItem());
            }
        });

        VBox root = new VBox(8, new HBox(6, bolumAdiBtn, aciklamaBtn, gruplarBtn),
                new HBox(8, ekleBtn, linkKontrolBtn, aramaAlani), sayiLabel, listView,
                new Label("Düzenlemek için çift tıkla. Silineni geri almak için Ctrl+Z."));
        root.setPadding(new Insets(10));
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root, 760, 620);
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                undoSil();
            }
        });
        stage.setScene(scene);
        hocalariYenile();
        stage.showAndWait();
    }

    private void jsonKaydet() {
        JsonUtil.write(hocalarJsonYolu, data);
    }

    private int grupSirasi(Hoca h) {
        for (int i = 0; i < data.gruplar.size(); i++) {
            if (data.gruplar.get(i).ad.equals(h.grup)) {
                return i;
            }
        }
        return data.gruplar.size();
    }

    private void hocalariYenile() {
        String q = aramaAlani.getText() == null ? "" : aramaAlani.getText().replace("İ", "i").toLowerCase();
        List<Hoca> gorunen = data.hocalar.stream()
                .filter(h -> q.isEmpty() || h.ad.replace("İ", "i").toLowerCase().contains(q)
                        || h.kisaltma.toLowerCase().contains(q))
                .sorted(Comparator.comparingInt(this::grupSirasi)
                        .thenComparing(h -> TurkceSira.unvansiz(h.ad), TurkceSira.KARSILASTIRICI))
                .toList();
        hocalarListModel.setAll(gorunen);
        sayiLabel.setText(gorunen.size() == data.hocalar.size()
                ? "Toplam " + gorunen.size() + " hoca" : gorunen.size() + " / " + data.hocalar.size() + " hoca");
    }

    /** Grubun README'de başlığın altında görünen açıklamasını düzenler ya da yeni grup ekler (sona). */
    private void gruplariDuzenle() {
        List<String> adlar = new java.util.ArrayList<>(data.gruplar.stream().map(g -> g.ad).toList());
        String yeniGrup = "➕ Yeni grup ekle";
        adlar.add(yeniGrup);
        Dialogs.choice("Gruplar", "Açıklamasını düzenlemek istediğin grup:", adlar, adlar.get(0)).ifPresent(secim -> {
            if (secim.equals(yeniGrup)) {
                Dialogs.textInput("Yeni Grup", "Grup adı (ör. Emekli Hocalar):", "").ifPresent(ad -> {
                    if (!ad.isBlank() && data.gruplar.stream().noneMatch(g -> g.ad.equals(ad.strip()))) {
                        HocaGrubu g = new HocaGrubu();
                        g.ad = ad.strip();
                        data.gruplar.add(g);
                        jsonKaydet();
                        Toast.show(stage, "Grup eklendi; hocalara Hoca Düzenle'den atayabilirsin.", Toast.Tip.BASARI);
                    }
                });
                return;
            }
            HocaGrubu g = HocaDuzenlemeDialog.grupBul(data, secim);
            Dialogs.multilineInput(secim, "Başlığın altındaki açıklama (boş bırakılabilir):", g.aciklama)
                    .ifPresent(yeni -> {
                        g.aciklama = yeni.strip();
                        jsonKaydet();
                    });
        });
    }

    private void undoSil() {
        var silinen = undoManager.popDeleted();
        if (silinen == null) {
            return;
        }
        data.hocalar.add(Math.min(silinen.index(), data.hocalar.size()), silinen.item());
        jsonKaydet();
        hocalariYenile();
        Toast.show(stage, "Hoca geri alındı.", Toast.Tip.BASARI);
    }

    private void hocaDuzenlemeAc(Hoca hoca) {
        DerslerBolumu dersler = JsonUtil.read(derslerJsonYolu, DerslerBolumu.class);
        new HocaDuzenlemeDialog(stage, hoca, data, dersler, derslerJsonYolu,
                () -> {
                    jsonKaydet();
                    hocalariYenile();
                    Toast.show(stage, "Hoca kaydedildi. README'ler için Git İşlemleri › Readme Güncelle.", Toast.Tip.BASARI);
                },
                silinenHoca -> {
                    undoManager.pushDeleted(data.hocalar.indexOf(silinenHoca), silinenHoca);
                    data.hocalar.remove(silinenHoca);
                    jsonKaydet();
                    hocalariYenile();
                    Toast.show(stage, "Hoca silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
                }).show();
    }
}
