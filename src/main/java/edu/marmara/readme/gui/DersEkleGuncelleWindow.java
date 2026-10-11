package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.MufredatAyari;
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

/**
 * Dersleri listeler, ekler, düzenler, siler ve sıralar. Listedeki sıra, derslerin ana README'de ve
 * dönem README'sinde görünme sırasıdır; ⬆️/⬇️ yalnızca aynı dönem/havuz içinde taşır.
 */
public class DersEkleGuncelleWindow {

    private static final String TUMU = "Tümü";

    private final Path derslerJsonYolu;
    private final Path hocalarJsonYolu;
    private final Path donemlerJsonYolu;
    private final Path konfigurasyonJsonYolu;

    private DerslerBolumu data;
    private DonemlerBolumu donemler;
    private final ObservableList<Ders> dersListModel = FXCollections.observableArrayList();
    private final UndoManager<Ders> undoManager = new UndoManager<>();

    private ListView<Ders> listView;
    private Label sayiLabel;
    private TextField aramaAlani;
    private ComboBox<String> klasorFiltresi;
    private Stage stage;

    public DersEkleGuncelleWindow(Path jsonDeposu) {
        this.derslerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DERSLER_JSON);
        this.hocalarJsonYolu = jsonDeposu.resolve(ReadmeGenerator.HOCALAR_JSON);
        this.donemlerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DONEMLER_JSON);
        this.konfigurasyonJsonYolu = jsonDeposu.resolve(ReadmeGenerator.KONFIGURASYON_JSON);
    }

    public void show() {
        data = JsonUtil.read(derslerJsonYolu, DerslerBolumu.class);
        donemler = JsonUtil.read(donemlerJsonYolu, DonemlerBolumu.class);
        if (data == null || donemler == null) {
            Dialogs.error("Hata", "dersler.json veya donemler.json bulunamadı. JSON klasörünü kontrol edin.");
            return;
        }

        stage = new Stage();
        stage.setTitle("Ders Ekle/Güncelle");
        stage.initModality(Modality.APPLICATION_MODAL);

        HBox metinler = new HBox(6,
                metinButonu("Dersler başlığı", () -> data.bolum_adi, s -> data.bolum_adi = s, false),
                metinButonu("Dersler açıklaması", () -> data.bolum_aciklamasi, s -> data.bolum_aciklamasi = s, true),
                metinButonu("Seçmeli başlığı", () -> data.secmeli_bolum_adi, s -> data.secmeli_bolum_adi = s, false),
                metinButonu("Seçmeli açıklaması", () -> data.secmeli_bolum_aciklamasi,
                        s -> data.secmeli_bolum_aciklamasi = s, true),
                metinButonu("Ders sayfası \"Katkıda Bulun\"", () -> data.katkida_bulun_metni,
                        s -> data.katkida_bulun_metni = s, true));

        Button ekleBtn = new Button("➕ Ders Ekle");
        ekleBtn.setOnAction(e -> dersDuzenlemeAc(null));
        Button yukariBtn = new Button("⬆️");
        yukariBtn.setOnAction(e -> tasi(-1));
        Button asagiBtn = new Button("⬇️");
        asagiBtn.setOnAction(e -> tasi(1));

        List<String> filtreler = new ArrayList<>(List.of(TUMU));
        donemler.donemler.forEach(d -> filtreler.add(d.klasor));
        donemler.havuzlar.forEach(h -> filtreler.add(h.klasor));
        klasorFiltresi = new ComboBox<>(FXCollections.observableArrayList(filtreler));
        klasorFiltresi.setValue(TUMU);
        klasorFiltresi.valueProperty().addListener((o, eski, yeni) -> dersleriYenile());

        aramaAlani = new TextField();
        aramaAlani.setPromptText("Ders ara...");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> dersleriYenile());
        HBox.setHgrow(aramaAlani, Priority.ALWAYS);

        sayiLabel = new Label();
        listView = new ListView<>(dersListModel);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ders ders, boolean empty) {
                super.updateItem(ders, empty);
                setText(empty || ders == null ? null
                        : ders.ustKlasor() + "  ›  " + ders.ad + "   (" + ders.mufredat + ")");
            }
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dersDuzenlemeAc(listView.getSelectionModel().getSelectedItem());
            }
        });

        VBox root = new VBox(8, metinler, new HBox(8, ekleBtn, yukariBtn, asagiBtn, klasorFiltresi, aramaAlani),
                sayiLabel, listView, new Label("Düzenlemek için çift tıkla. Silineni geri almak için Ctrl+Z."));
        root.setPadding(new Insets(10));
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root, 860, 640);
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                undoSil();
            }
        });
        stage.setScene(scene);
        dersleriYenile();
        stage.showAndWait();
    }

    private Button metinButonu(String ad, Supplier<String> al, Consumer<String> yaz, boolean cokSatir) {
        Button b = new Button(ad);
        b.setTooltip(new Tooltip(MetinIslemleri.kisaltMetin(al.get())));
        b.setOnAction(e -> {
            var sonuc = cokSatir ? Dialogs.multilineInput(ad, ad + ":", al.get()) : Dialogs.textInput(ad, ad + ":", al.get());
            sonuc.ifPresent(yeni -> {
                if (!yeni.equals(al.get())) {
                    yaz.accept(yeni);
                    jsonKaydet();
                    b.setTooltip(new Tooltip(MetinIslemleri.kisaltMetin(yeni)));
                }
            });
        });
        return b;
    }

    /** Seçili dersi aynı dönem/havuzdaki komşusuyla yer değiştirir. */
    private void tasi(int yon) {
        Ders secili = listView.getSelectionModel().getSelectedItem();
        if (secili == null) {
            return;
        }
        List<Ders> l = data.dersler;
        int i = l.indexOf(secili);
        int j = i + yon;
        while (j >= 0 && j < l.size() && !l.get(j).ustKlasor().equals(secili.ustKlasor())) {
            j += yon;
        }
        if (j < 0 || j >= l.size()) {
            return;
        }
        l.set(i, l.get(j));
        l.set(j, secili);
        jsonKaydet();
        dersleriYenile();
        listView.getSelectionModel().select(secili);
    }

    private void undoSil() {
        var silinen = undoManager.popDeleted();
        if (silinen == null) {
            return;
        }
        data.dersler.add(Math.min(silinen.index(), data.dersler.size()), silinen.item());
        jsonKaydet();
        dersleriYenile();
        Toast.show(stage, "Ders geri alındı.", Toast.Tip.BASARI);
    }

    private void jsonKaydet() {
        JsonUtil.write(derslerJsonYolu, data);
    }

    private void dersleriYenile() {
        String klasor = klasorFiltresi.getValue();
        String q = aramaAlani.getText() == null ? "" : aramaAlani.getText().replace("İ", "i").toLowerCase();
        List<Ders> gorunen = data.dersler.stream()
                .filter(d -> TUMU.equals(klasor) || d.ustKlasor().equals(klasor))
                .filter(d -> q.isEmpty() || d.ad.replace("İ", "i").toLowerCase().contains(q))
                .toList();
        dersListModel.setAll(gorunen);
        sayiLabel.setText(gorunen.size() == data.dersler.size()
                ? "Toplam " + gorunen.size() + " ders" : gorunen.size() + " / " + data.dersler.size() + " ders");
    }

    private void dersDuzenlemeAc(Ders ders) {
        HocalarBolumu hocalar = JsonUtil.read(hocalarJsonYolu, HocalarBolumu.class);
        Konfigurasyon konfig = JsonUtil.read(konfigurasyonJsonYolu, Konfigurasyon.class);
        new DersDuzenlemeDialog(stage, ders, data, hocalar, donemler,
                konfig != null ? konfig.mufredat : new MufredatAyari(),
                () -> {
                    jsonKaydet();
                    dersleriYenile();
                    Toast.show(stage, "Ders kaydedildi. README'ler için Git İşlemleri › Readme Güncelle.", Toast.Tip.BASARI);
                },
                silinenDers -> {
                    undoManager.pushDeleted(data.dersler.indexOf(silinenDers), silinenDers);
                    data.dersler.remove(silinenDers);
                    jsonKaydet();
                    dersleriYenile();
                    Toast.show(stage, "Ders silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
                }).show();
    }
}
