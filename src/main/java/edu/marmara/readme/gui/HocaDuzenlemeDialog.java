package edu.marmara.readme.gui;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocaGrubu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.util.HocaKisaltmaOlustur;
import edu.marmara.readme.engine.util.JsonUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tek bir hocayı ekler/düzenler. Hocanın hangi dersleri hangi yıl verdiği ders tarafında
 * (Ders Ekle/Güncelle › yıl yıl hocalar) girilir; burada yalnızca gösterilir. Hocanın adı
 * değişirse derslerdeki tüm kayıtları da yeni ada çevrilir.
 */
public class HocaDuzenlemeDialog {

    /** Uzun ünvanlar önce: "Öğr. Gör. Dr." "Öğr. Gör."den önce denenmeli. */
    static final List<String> UNVANLAR = List.of("Prof. Dr.", "Prof.", "Doç. Dr.", "Dr. Öğretim Üyesi",
            "Öğr. Gör. Dr.", "Öğr. Gör.", "Arş. Gör. Dr.", "Arş. Gör.", "Dr.");
    private static final String UNVANSIZ = "(ünvansız)";
    private static final List<String> CINSIYETLER = List.of("Erkek (👨‍🏫)", "Kadın (👩‍🏫)", "Belirtilmemiş (🧑‍🏫)");

    private final Stage owner;
    private final Hoca duzenlenecekHoca; // null ise "ekle" modu
    private final HocalarBolumu hocalarData;
    private final DerslerBolumu derslerData;
    private final Path derslerJsonYolu;
    private final Runnable onSaved;
    private final Consumer<Hoca> onDeleted;

    private ComboBox<String> unvanBox;
    private TextField adAlani;
    private TextField kisaltmaAlani;
    private ComboBox<String> grupBox;
    private TextField ofisAlani;
    private TextField linkAlani;
    private CheckBox aktifKutusu;
    private ComboBox<String> cinsiyetBox;
    private boolean kisaltmaElleDegisti;

    public HocaDuzenlemeDialog(Stage owner, Hoca hoca, HocalarBolumu hocalarData, DerslerBolumu derslerData,
                               Path derslerJsonYolu, Runnable onSaved, Consumer<Hoca> onDeleted) {
        this.owner = owner;
        this.duzenlenecekHoca = hoca;
        this.hocalarData = hocalarData;
        this.derslerData = derslerData != null ? derslerData : new DerslerBolumu();
        this.derslerJsonYolu = derslerJsonYolu;
        this.onSaved = onSaved;
        this.onDeleted = onDeleted;
    }

    public void show() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(duzenlenecekHoca != null ? "Hoca Düzenle" : "Hoca Ekle");
        Hoca h = duzenlenecekHoca != null ? duzenlenecekHoca : new Hoca();

        String unvan = unvanBul(h.ad);
        List<String> unvanlar = new ArrayList<>(UNVANLAR);
        unvanlar.add(UNVANSIZ);
        unvanBox = new ComboBox<>(FXCollections.observableArrayList(unvanlar));
        unvanBox.setValue(duzenlenecekHoca == null ? "Dr. Öğretim Üyesi" : unvan.isEmpty() ? UNVANSIZ : unvan);
        adAlani = new TextField(unvan.isEmpty() ? h.ad : h.ad.substring(unvan.length()).strip());

        kisaltmaAlani = new TextField(h.kisaltma);
        kisaltmaAlani.textProperty().addListener((o, eski, yeni) -> kisaltmaElleDegisti = kisaltmaAlani.isFocused());
        if (duzenlenecekHoca == null) {
            adAlani.textProperty().addListener((o, eski, yeni) -> kisaltmaOner());
        }

        grupBox = new ComboBox<>(FXCollections.observableArrayList(
                hocalarData.gruplar.stream().map(g -> g.ad).toList()));
        grupBox.setValue(duzenlenecekHoca != null ? h.grup : null);
        if (duzenlenecekHoca == null) {
            grupOner(unvanBox.getValue());
        }
        // Ünvan değişince (ör. terfi) grup da ünvana göre değişir; elle seçilmiş özel gruba dokunulmaz.
        unvanBox.valueProperty().addListener((o, eski, yeni) -> {
            if (grupBox.getValue() == null || grupBox.getValue().equals(varsayilanGrup(eski))) {
                grupOner(yeni);
            }
        });
        ofisAlani = new TextField(h.ofis);
        ofisAlani.setPromptText("ör. T4-106");
        linkAlani = new TextField(h.link);
        linkAlani.setPromptText("ör. https://avesis.marmara.edu.tr/...");
        aktifKutusu = new CheckBox("Aktif görevde");
        aktifKutusu.setSelected(h.hoca_aktif_gorevde_mi);
        cinsiyetBox = new ComboBox<>(FXCollections.observableArrayList(CINSIYETLER));
        cinsiyetBox.setValue(h.erkek_mi == null ? CINSIYETLER.get(2) : h.erkek_mi ? CINSIYETLER.get(0) : CINSIYETLER.get(1));

        ListView<String> dersListesi = new ListView<>(FXCollections.observableArrayList(verdigiDersler(h.ad)));
        dersListesi.setPrefHeight(130);

        Button kaydetBtn = new Button(duzenlenecekHoca != null ? "Değişiklikleri Kaydet" : "Ekle");
        kaydetBtn.setOnAction(e -> kaydet(stage));
        HBox altButonlar = new HBox(8, kaydetBtn);
        if (duzenlenecekHoca != null) {
            Button silBtn = new Button("Hocayı Sil");
            silBtn.setOnAction(e -> sil(stage));
            altButonlar.getChildren().add(silBtn);
        }

        VBox root = new VBox(6,
                new Label("Ünvan"), unvanBox,
                new Label("Ad Soyad"), adAlani,
                new Label("Kısaltma (ders kartlarında hocaya link olarak görünür)"), kisaltmaAlani,
                new Label("Ana README'deki grup"), grupBox,
                new Label("Ofis"), ofisAlani,
                new Label("Araştırma sayfası (AVESİS)"), linkAlani,
                aktifKutusu,
                new Label("Cinsiyet (kart emojisi)"), cinsiyetBox,
                new Label("Verdiği dersler (Ders Ekle/Güncelle'deki yıl yıl hoca bilgisinden)"), dersListesi,
                altButonlar);
        root.setPadding(new Insets(12));

        String ilk = durum();
        CloseGuard.install(stage, () -> !durum().equals(ilk));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 520, 720));
        stage.showAndWait();
    }

    private String durum() {
        return unvanBox.getValue() + "|" + adAlani.getText() + "|" + kisaltmaAlani.getText() + "|" + grupBox.getValue()
                + "|" + ofisAlani.getText() + "|" + linkAlani.getText() + "|" + aktifKutusu.isSelected()
                + "|" + cinsiyetBox.getValue();
    }

    static String unvanBul(String ad) {
        for (String u : UNVANLAR) {
            if (ad.startsWith(u + " ")) {
                return u;
            }
        }
        return "";
    }

    private String tamAd() {
        String ad = adAlani.getText().strip();
        return UNVANSIZ.equals(unvanBox.getValue()) ? ad : unvanBox.getValue() + " " + ad;
    }

    private void kisaltmaOner() {
        if (kisaltmaElleDegisti || adAlani.getText().isBlank()) {
            return;
        }
        List<String> mevcut = hocalarData.hocalar.stream().map(x -> x.kisaltma).toList();
        kisaltmaAlani.setText(HocaKisaltmaOlustur.oner(tamAd(), mevcut));
    }

    /** Ünvanın ana README'deki varsayılan grubu. */
    static String varsayilanGrup(String unvan) {
        return unvan == null ? null : unvan.startsWith("Prof.") ? "Profesörler"
                : unvan.startsWith("Doç.") ? "Doçentler"
                : unvan.equals("Dr. Öğretim Üyesi") ? "Doktor Öğretim Üyeleri"
                : unvan.startsWith("Arş. Gör.") ? "Araştırma Görevlileri" : "Öğretim Görevlileri";
    }

    private void grupOner(String unvan) {
        String grup = varsayilanGrup(unvan);
        if (hocalarData.gruplar.stream().anyMatch(g -> g.ad.equals(grup))) {
            grupBox.setValue(grup);
        }
    }

    /** "Ders adı — 2024-2025, 2025-2026" satırları. */
    private List<String> verdigiDersler(String hocaAdi) {
        Map<String, List<String>> yillar = new LinkedHashMap<>();
        for (Ders d : derslerData.dersler) {
            for (AkademisyenYili y : d.akademisyenler) {
                if (y.hocalar.stream().anyMatch(x -> x.ad.equals(hocaAdi))) {
                    yillar.computeIfAbsent(d.ad + " (" + d.ustKlasor() + ")", k -> new ArrayList<>()).add(0, y.yil);
                }
            }
        }
        List<String> satirlar = new ArrayList<>();
        yillar.forEach((ders, yl) -> satirlar.add(ders + " — " + String.join(", ", yl)));
        return satirlar;
    }

    private int dersKaydiSayisi(String hocaAdi) {
        int n = 0;
        for (Ders d : derslerData.dersler) {
            for (AkademisyenYili y : d.akademisyenler) {
                n += (int) y.hocalar.stream().filter(x -> x.ad.equals(hocaAdi)).count();
            }
        }
        return n;
    }

    private void kaydet(Stage stage) {
        String ad = tamAd();
        String kisaltma = kisaltmaAlani.getText().strip();
        if (adAlani.getText().isBlank() || kisaltma.isEmpty() || grupBox.getValue() == null) {
            Dialogs.error("Hata", "Ad, kısaltma ve grup boş olamaz.");
            return;
        }
        boolean yeni = duzenlenecekHoca == null;
        if (hocalarData.hocalar.stream().anyMatch(x -> x != duzenlenecekHoca && x.ad.equals(ad))) {
            Dialogs.error("Hata", "Bu isimde bir hoca zaten var!");
            return;
        }
        Hoca ayniKisaltma = hocalarData.hocalar.stream()
                .filter(x -> x != duzenlenecekHoca && x.kisaltma.equals(kisaltma)).findFirst().orElse(null);
        if (ayniKisaltma != null) {
            Dialogs.error("Hata", "\"" + kisaltma + "\" kısaltması " + ayniKisaltma.ad + " için kullanılıyor.");
            return;
        }

        Hoca hoca = yeni ? new Hoca() : duzenlenecekHoca;
        String eskiAd = hoca.ad;
        hoca.ad = ad;
        hoca.kisaltma = kisaltma;
        hoca.grup = grupBox.getValue();
        hoca.ofis = ofisAlani.getText().strip();
        hoca.link = linkAlani.getText().strip();
        hoca.hoca_aktif_gorevde_mi = aktifKutusu.isSelected();
        int c = CINSIYETLER.indexOf(cinsiyetBox.getValue());
        hoca.erkek_mi = c == 0 ? Boolean.TRUE : c == 1 ? Boolean.FALSE : null;
        if (yeni) {
            hocalarData.hocalar.add(hoca);
        } else if (!eskiAd.equals(ad) && dersKaydiSayisi(eskiAd) > 0) {
            derslerdeAdiDegistir(eskiAd, ad);
        }
        onSaved.run();
        stage.close();
    }

    private void derslerdeAdiDegistir(String eskiAd, String yeniAd) {
        for (Ders d : derslerData.dersler) {
            for (AkademisyenYili y : d.akademisyenler) {
                for (DersHocasi x : y.hocalar) {
                    if (x.ad.equals(eskiAd)) {
                        x.ad = yeniAd;
                    }
                }
            }
        }
        JsonUtil.write(derslerJsonYolu, derslerData);
    }

    private void sil(Stage stage) {
        int n = dersKaydiSayisi(duzenlenecekHoca.ad);
        String uyari = n == 0 ? "Hocayı silmek istediğinden emin misin?"
                : "Bu hoca derslerde " + n + " yerde geçiyor; orada adı kısaltmasız düz metin olarak kalır. "
                + "Yine de silinsin mi?";
        if (!Dialogs.confirm("Onay", uyari)) {
            return;
        }
        onDeleted.accept(duzenlenecekHoca);
        stage.close();
    }

    /** Grup açıklaması gibi alanları düzenlemek için grup listesindeki adı bulur. */
    static HocaGrubu grupBul(HocalarBolumu hocalar, String ad) {
        return hocalar.gruplar.stream().filter(g -> g.ad.equals(ad)).findFirst().orElse(null);
    }
}
