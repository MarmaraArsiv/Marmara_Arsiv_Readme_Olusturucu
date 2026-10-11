package edu.marmara.readme.gui;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.MufredatAyari;
import edu.marmara.readme.engine.model.OneriGrubu;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Tek bir dersi ekler/düzenler: klasör, tip, müfredat ve notu, yıl yıl hocalar, öneriler. */
public class DersDuzenlemeDialog {

    private final Stage owner;
    private final Ders duzenlenecekDers; // null ise "ekle" modu
    private final DerslerBolumu derslerData;
    private final HocalarBolumu hocalarData;
    private final DonemlerBolumu donemlerData;
    private final MufredatAyari mufredatAyari;
    private final Runnable onSaved;
    private final Consumer<Ders> onDeleted;

    private TextField adAlani;
    private ComboBox<String> ustKlasorBox;
    private ComboBox<String> tipBox;
    private ComboBox<String> mufredatBox;
    private TextField mufredatNotuAlani;
    private final List<AkademisyenYili> yillar = new ArrayList<>();
    private final ObservableList<String> yilGosterim = FXCollections.observableArrayList();
    private ListView<String> yilListView;
    private final List<String[]> oneriCiftleri = new ArrayList<>(); // [sahibi, oneri]
    private final ObservableList<String> oneriGosterim = FXCollections.observableArrayList();
    private ListView<String> oneriListView;

    public DersDuzenlemeDialog(Stage owner, Ders ders, DerslerBolumu derslerData, HocalarBolumu hocalarData,
                               DonemlerBolumu donemlerData, MufredatAyari mufredatAyari, Runnable onSaved,
                               Consumer<Ders> onDeleted) {
        this.owner = owner;
        this.duzenlenecekDers = ders;
        this.derslerData = derslerData;
        this.hocalarData = hocalarData != null ? hocalarData : new HocalarBolumu();
        this.donemlerData = donemlerData != null ? donemlerData : new DonemlerBolumu();
        this.mufredatAyari = mufredatAyari;
        this.onSaved = onSaved;
        this.onDeleted = onDeleted;
    }

    public void show() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(duzenlenecekDers != null ? "Ders Düzenle" : "Ders Ekle");
        Ders d = duzenlenecekDers != null ? duzenlenecekDers : new Ders();

        adAlani = new TextField(d.ad);

        List<String> ustKlasorler = new ArrayList<>();
        donemlerData.donemler.forEach(x -> ustKlasorler.add(x.klasor));
        donemlerData.havuzlar.forEach(x -> ustKlasorler.add(x.klasor));
        ustKlasorBox = new ComboBox<>(FXCollections.observableArrayList(ustKlasorler));
        ustKlasorBox.setValue(duzenlenecekDers != null ? d.ustKlasor() : ustKlasorler.isEmpty() ? null : ustKlasorler.get(0));
        // Var olan dersin klasörü değiştirilemez: README eski klasörde kalır, dosyalar taşınmaz.
        ustKlasorBox.setDisable(duzenlenecekDers != null);
        Label klasorBilgisi = new Label(duzenlenecekDers != null
                ? "Klasör: " + d.klasor + " (klasörü değiştirmek için önce klasörü taşıyın)"
                : "Klasör: <dönem veya havuz>/<ders adı> olarak oluşturulur");

        tipBox = new ComboBox<>(FXCollections.observableArrayList("Zorunlu", "Seçmeli"));
        tipBox.setEditable(true);
        tipBox.setValue(duzenlenecekDers != null ? d.tip : havuzMu(ustKlasorBox.getValue()) ? "Seçmeli" : "Zorunlu");
        ustKlasorBox.valueProperty().addListener((o, eski, yeni) -> {
            if (duzenlenecekDers == null) {
                tipBox.setValue(havuzMu(yeni) ? "Seçmeli" : "Zorunlu");
            }
        });

        // Seçenekler bölümün konfigurasyon.json'undan (Konfigürasyon penceresinden düzenlenir); ilki varsayılan.
        Set<String> mufredatlar = new LinkedHashSet<>(mufredatAyari.secenekler);
        derslerData.dersler.forEach(x -> mufredatlar.add(x.mufredat));
        mufredatBox = new ComboBox<>(FXCollections.observableArrayList(mufredatlar));
        mufredatBox.setEditable(true);
        mufredatBox.setValue(duzenlenecekDers != null ? d.mufredat
                : mufredatlar.isEmpty() ? "" : mufredatlar.iterator().next());
        mufredatNotuAlani = new TextField(d.mufredat_notu);
        mufredatNotuAlani.setPromptText("İsteğe bağlı, ör. yeni müfredatta 2. yarıyılda (1-2) yer alıyor.");

        for (AkademisyenYili y : d.akademisyenler) {
            yillar.add(kopya(y));
        }
        yilListView = new ListView<>(yilGosterim);
        yilListView.setPrefHeight(150);
        yilListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                yilDuzenle();
            }
        });
        Button yilEkleBtn = new Button("Yıl Ekle");
        yilEkleBtn.setOnAction(e -> yilEkle());
        Button yilDuzenleBtn = new Button("Seçili Yılı Düzenle");
        yilDuzenleBtn.setOnAction(e -> yilDuzenle());
        Button yilSilBtn = new Button("Seçili Yılı Sil");
        yilSilBtn.setOnAction(e -> {
            int i = yilListView.getSelectionModel().getSelectedIndex();
            if (i >= 0 && Dialogs.confirm("Onay", yillar.get(i).yil + " yılını silmek istediğine emin misin?")) {
                yillar.remove(i);
                yillariGoster();
            }
        });
        yillariGoster();

        for (OneriGrubu grup : d.derse_dair_oneriler) {
            for (String oneri : grup.oneriler) {
                oneriCiftleri.add(new String[]{grup.oneri_sahibi, oneri});
            }
        }
        oneriCiftleri.forEach(p -> oneriGosterim.add(p[0] + ": " + p[1]));
        oneriListView = new ListView<>(oneriGosterim);
        oneriListView.setPrefHeight(80);
        Button oneriEkleBtn = new Button("Öneri Ekle");
        oneriEkleBtn.setOnAction(e -> oneriEkle());
        Button oneriSilBtn = new Button("Seçili Öneriyi Kaldır");
        oneriSilBtn.setOnAction(e -> {
            int idx = oneriListView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                oneriCiftleri.remove(idx);
                oneriGosterim.remove(idx);
            }
        });

        Button kaydetBtn = new Button(duzenlenecekDers != null ? "Değişiklikleri Kaydet" : "Ekle");
        kaydetBtn.setOnAction(e -> kaydet(stage));
        HBox altButonlar = new HBox(8, kaydetBtn);
        if (duzenlenecekDers != null) {
            Button silBtn = new Button("Dersi Sil");
            silBtn.setOnAction(e -> sil(stage));
            altButonlar.getChildren().add(silBtn);
        }

        VBox root = new VBox(6,
                new Label("Ders Adı"), adAlani,
                new Label("Dönem / Seçmeli Havuzu"), ustKlasorBox, klasorBilgisi,
                new Label("Ders Tipi"), tipBox,
                new Label("Müfredat"), mufredatBox,
                new Label("Müfredat Notu (ℹ️ satırı)"), mufredatNotuAlani,
                new Label("Dersi Yürüten Akademisyenler (yıl yıl; ana sayfada en yeni yıl görünür)"), yilListView,
                new HBox(8, yilEkleBtn, yilDuzenleBtn, yilSilBtn),
                new Label("Derse Dair Öneriler"), oneriListView, new HBox(8, oneriEkleBtn, oneriSilBtn),
                altButonlar);
        root.setPadding(new Insets(12));

        String ilk = durum();
        CloseGuard.install(stage, () -> !durum().equals(ilk));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 620, 760));
        stage.showAndWait();
    }

    /** Kaydedilmemiş değişiklik kontrolü için formun özeti. */
    private String durum() {
        StringBuilder sb = new StringBuilder();
        sb.append(adAlani.getText()).append('|').append(ustKlasorBox.getValue()).append('|').append(tipBox.getValue())
                .append('|').append(mufredatBox.getValue()).append('|').append(mufredatNotuAlani.getText());
        yilGosterim.forEach(s -> sb.append('|').append(s));
        oneriGosterim.forEach(s -> sb.append('|').append(s));
        return sb.toString();
    }

    private boolean havuzMu(String ustKlasor) {
        return donemlerData.havuzlar.stream().anyMatch(h -> h.klasor.equals(ustKlasor));
    }

    private static AkademisyenYili kopya(AkademisyenYili y) {
        AkademisyenYili k = new AkademisyenYili();
        k.yil = y.yil;
        y.hocalar.forEach(h -> k.hocalar.add(new DersHocasi(h.ad, h.not)));
        return k;
    }

    private void yillariGoster() {
        yillar.sort(Comparator.comparing((AkademisyenYili y) -> y.yil).reversed());
        yilGosterim.setAll(yillar.stream().map(y -> y.yil + ":  " + String.join(", ",
                y.hocalar.stream().map(h -> h.not.isEmpty() ? h.ad : h.ad + " (" + h.not + ")").toList())).toList());
    }

    private void yilEkle() {
        String oneri = yillar.isEmpty() ? "2026-2027" : sonrakiYil(yillar.get(0).yil);
        new AkademisyenYiliDialog(yilListView.getScene().getWindow(), null, oneri, hocalarData, yillar)
                .show().ifPresent(y -> {
                    yillar.add(y);
                    yillariGoster();
                });
    }

    private void yilDuzenle() {
        int i = yilListView.getSelectionModel().getSelectedIndex();
        if (i < 0) {
            return;
        }
        new AkademisyenYiliDialog(yilListView.getScene().getWindow(), yillar.get(i), null, hocalarData, yillar)
                .show().ifPresent(y -> {
                    yillar.set(i, y);
                    yillariGoster();
                });
    }

    private static String sonrakiYil(String yil) {
        try {
            int bas = Integer.parseInt(yil.substring(0, 4)) + 1;
            return bas + "-" + (bas + 1);
        } catch (RuntimeException e) {
            return "";
        }
    }

    private void oneriEkle() {
        Dialogs.textInput("Öneri Sahibi", "Öneri sahibinin adı/nicki:", "Anonim").ifPresent(sahibi ->
                Dialogs.multilineInput("Öneri", "Dersle ilgili öneri:", "").ifPresent(metin -> {
                    if (!metin.isBlank()) {
                        String s = sahibi.isBlank() ? "Anonim" : sahibi;
                        oneriCiftleri.add(new String[]{s, metin});
                        oneriGosterim.add(s + ": " + metin);
                    }
                }));
    }

    private void kaydet(Stage stage) {
        String ad = adAlani.getText().strip();
        String ust = ustKlasorBox.getValue();
        if (ad.isEmpty() || ust == null) {
            Dialogs.error("Hata", "Ders adı ve dönem/havuz boş olamaz.");
            return;
        }
        if (ad.contains("/")) {
            Dialogs.error("Hata", "Ders adında \"/\" olamaz (klasör adı olarak kullanılıyor).");
            return;
        }
        String mufredat = mufredatBox.getValue() == null ? "" : mufredatBox.getValue().strip();
        if (mufredat.isEmpty()) {
            Dialogs.error("Hata", "Müfredat boş olamaz.");
            return;
        }
        boolean yeni = duzenlenecekDers == null;
        String klasor = yeni ? ust + "/" + ad : duzenlenecekDers.klasor;
        if (yeni && derslerData.dersler.stream().anyMatch(x -> x.klasor.equals(klasor))) {
            Dialogs.error("Hata", klasor + " klasöründe zaten bir ders var.");
            return;
        }

        Ders ders = yeni ? new Ders() : duzenlenecekDers;
        ders.ad = ad;
        ders.klasor = klasor;
        Donem donem = donemlerData.donemler.stream().filter(x -> x.klasor.equals(ust)).findFirst().orElse(null);
        ders.yil = donem != null ? donem.yil : 0;
        ders.donem = donem != null ? donem.donem : "";
        ders.tip = tipBox.getValue() == null ? "" : tipBox.getValue().strip();
        ders.mufredat = mufredat;
        ders.mufredat_notu = mufredatNotuAlani.getText().strip();
        ders.akademisyenler = new ArrayList<>(yillar);

        Map<String, OneriGrubu> gruplar = new LinkedHashMap<>();
        for (String[] cift : oneriCiftleri) {
            gruplar.computeIfAbsent(cift[0], sahibi -> {
                OneriGrubu g = new OneriGrubu();
                g.oneri_sahibi = sahibi;
                return g;
            }).oneriler.add(cift[1]);
        }
        ders.derse_dair_oneriler = new ArrayList<>(gruplar.values());

        if (yeni) {
            // Aynı dönem/havuzdaki son dersin arkasına; yoksa listenin sonuna.
            int konum = derslerData.dersler.size();
            for (int i = 0; i < derslerData.dersler.size(); i++) {
                if (derslerData.dersler.get(i).ustKlasor().equals(ust)) {
                    konum = i + 1;
                }
            }
            derslerData.dersler.add(konum, ders);
        }
        onSaved.run();
        stage.close();
    }

    private void sil(Stage stage) {
        if (!Dialogs.confirm("Onay", "Dersi silmek istediğinden emin misin? (Klasör ve dosyalar silinmez, "
                + "yalnızca README'lerden çıkar.)")) {
            return;
        }
        onDeleted.accept(duzenlenecekDers);
        stage.close();
    }
}
