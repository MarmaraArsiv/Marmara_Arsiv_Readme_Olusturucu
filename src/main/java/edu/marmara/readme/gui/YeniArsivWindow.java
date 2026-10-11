package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ArsivSablonu;
import edu.marmara.readme.engine.model.MufredatAyari;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Yeni bölüm arşivi sihirbazı: bölüm bilgilerini alır, arşivi aracın şablonundan kurar (dönem/havuz klasörleri,
 * JSON'lar, GitHub betikleri, Google Form betikleri) ve README'leri üretir. Sonraki adımlar
 * .github/google_form/KURULUM.md'de.
 */
public class YeniArsivWindow {

    private final Stage owner;
    private Path olusan;

    public YeniArsivWindow(Stage owner) {
        this.owner = owner;
    }

    /** Sihirbazı açar; arşiv oluşturulduysa JSON klasörünü, vazgeçildiyse null döner. */
    public Path show() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Yeni Bölüm Arşivi");

        TextField bolumAlani = new TextField();
        bolumAlani.setPromptText("ör. Elektrik-Elektronik Mühendisliği");
        ComboBox<String> fakulteBox = new ComboBox<>(FXCollections.observableArrayList(
                "Teknoloji Fakültesi", "Mühendislik Fakültesi"));
        fakulteBox.setEditable(true);
        fakulteBox.setValue("Teknoloji Fakültesi");
        TextField githubAlani = new TextField();
        githubAlani.setPromptText("https://github.com/MarmaraArsiv/tek_elektrik");
        TextField driveAlani = new TextField();
        driveAlani.setPromptText("Drive'daki bölüm klasörü, ör. Elektrik-Teknoloji");

        // Bölüm/fakülte yazıldıkça repo adresi ve Drive klasörü önerilir; elle değiştirilince öneri durur.
        boolean[] elleDegisti = {false, false};
        githubAlani.setOnKeyTyped(e -> elleDegisti[0] = true);
        driveAlani.setOnKeyTyped(e -> elleDegisti[1] = true);
        Runnable oner = () -> {
            String fakulte = fakulteBox.getEditor().getText();
            if (!elleDegisti[0]) {
                githubAlani.setText(repoOnerisi(bolumAlani.getText(), fakulte));
            }
            if (!elleDegisti[1]) {
                driveAlani.setText(driveOnerisi(bolumAlani.getText(), fakulte));
            }
        };
        bolumAlani.textProperty().addListener((o, a, b) -> oner.run());
        fakulteBox.getEditor().textProperty().addListener((o, a, b) -> oner.run());

        Path varsayilanUst = Path.of("..").toAbsolutePath().normalize();
        TextField ustKlasorAlani = new TextField(varsayilanUst.toString());
        Button ustKlasorSecBtn = new Button("Seç…");
        ustKlasorSecBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File secilen = chooser.showDialog(stage);
            if (secilen != null) {
                ustKlasorAlani.setText(secilen.getAbsolutePath());
            }
        });
        HBox.setHgrow(ustKlasorAlani, Priority.ALWAYS);
        HBox ustKlasorSatiri = new HBox(6, ustKlasorAlani, ustKlasorSecBtn);
        Label hedefLabel = new Label();
        Runnable hedefGoster = () -> hedefLabel.setText("Oluşturulacak klasör: " + hedef(ustKlasorAlani.getText(), githubAlani.getText()));
        githubAlani.textProperty().addListener((o, a, b) -> hedefGoster.run());
        ustKlasorAlani.textProperty().addListener((o, a, b) -> hedefGoster.run());
        hedefGoster.run();

        Spinner<Integer> sinifSpinner = new Spinner<>(1, 6, 4);
        TextArea havuzAlani = new TextArea("Mesleki Seçmeli\nÜniversite Seçmeli\nFakülte Teknik Seçmeli");
        havuzAlani.setPrefRowCount(3);

        MufredatAyari varsayilan = new MufredatAyari();
        TextArea mufredatAlani = new TextArea(String.join("\n", varsayilan.secenekler));
        mufredatAlani.setPrefRowCount(4);
        TextField eskiOnekAlani = new TextField(varsayilan.eski_mufredat_oneki);
        TextField eskiYilAlani = new TextField(varsayilan.eski_mufredat_notu_yili);
        TextField eskiNotAlani = new TextField(varsayilan.eski_mufredat_notu);

        Button olusturBtn = new Button("Oluştur");
        olusturBtn.setDefaultButton(true);
        olusturBtn.setOnAction(e -> {
            MufredatAyari m = new MufredatAyari();
            m.secenekler = satirlar(mufredatAlani.getText());
            m.eski_mufredat_oneki = eskiOnekAlani.getText().strip();
            m.eski_mufredat_notu_yili = eskiYilAlani.getText().strip();
            m.eski_mufredat_notu = eskiNotAlani.getText().strip();
            var bilgi = new ArsivSablonu.BolumBilgisi(bolumAlani.getText(), fakulteBox.getEditor().getText(),
                    githubAlani.getText(), sinifSpinner.getValue(), satirlar(havuzAlani.getText()), m,
                    driveAlani.getText());
            List<String> hatalar = ArsivSablonu.bilgiHatalari(bilgi);
            if (!hatalar.isEmpty()) {
                Dialogs.error("Eksik Bilgi", String.join("\n", hatalar));
                return;
            }
            Path hedef = hedef(ustKlasorAlani.getText(), githubAlani.getText());
            try {
                olusan = ArsivSablonu.yeniArsivOlustur(hedef, bilgi, s -> { });
            } catch (RuntimeException ex) {
                Dialogs.error("Oluşturulamadı", ex.getMessage());
                return;
            }
            Dialogs.info("Arşiv Oluşturuldu", hedef + " hazır.\n\nSonraki adımlar (GitHub reposu, Actions ayarları, "
                    + "Google Formları, Telegram botu) arşivdeki .github/google_form/KURULUM.md dosyasında.\n"
                    + "Dersleri ve hocaları bu araçla ekleyip Git İşlemleri'nden pushlayabilirsin.");
            stage.close();
        });
        Button vazgecBtn = new Button("Vazgeç");
        vazgecBtn.setCancelButton(true);
        vazgecBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(6,
                new Label("Bölüm Adı"), bolumAlani,
                new Label("Fakülte"), fakulteBox,
                new Label("GitHub Reposu"), githubAlani,
                new Label("Drive Klasörü (Katkıda Bulunun bölümünde geçer)"), driveAlani,
                new Label("Arşivin Konulacağı Klasör"), ustKlasorSatiri, hedefLabel,
                new Label("Sınıf Sayısı (her sınıf için Güz ve Bahar dönem klasörleri açılır)"), sinifSpinner,
                new Label("Seçmeli Havuzları (her satıra bir tane, boş bırakılabilir)"), havuzAlani,
                new Label("Müfredat Seçenekleri (her satıra bir tane; ilki yeni derslerin varsayılanı)"), mufredatAlani,
                new Label("Eski Müfredat Öneki (boşsa eski müfredat notu yazılmaz)"), eskiOnekAlani,
                new Label("Eski Müfredat Notu Başlangıç Yılı"), eskiYilAlani,
                new Label("Eski Müfredat Notu"), eskiNotAlani,
                new HBox(8, olusturBtn, vazgecBtn));
        root.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 560, 720));
        stage.showAndWait();
        return olusan;
    }

    /** Üst klasör + repo adı (GitHub adresinin son parçası); adres boşsa "yeni_arsiv". */
    static Path hedef(String ustKlasor, String githubUrl) {
        String ad = githubUrl.strip().replaceAll("(\\.git)?/*$", "");
        ad = ad.substring(ad.lastIndexOf('/') + 1);
        return Path.of(ustKlasor.strip()).resolve(ad.isBlank() ? "yeni_arsiv" : ad).toAbsolutePath().normalize();
    }

    /** "Elektrik-Elektronik Mühendisliği", "Teknoloji Fakültesi" → https://github.com/MarmaraArsiv/tek_elektrik */
    static String repoOnerisi(String bolum, String fakulte) {
        String ilk = asciiKucuk(ilkKelime(bolum));
        if (ilk.isEmpty()) {
            return "";
        }
        String f = asciiKucuk(ilkKelime(fakulte));
        return "https://github.com/MarmaraArsiv/" + (f.isEmpty() ? "" : f.substring(0, Math.min(3, f.length())) + "_") + ilk;
    }

    /** "Elektrik-Elektronik Mühendisliği", "Teknoloji Fakültesi" → "Elektrik-Teknoloji" (Bilgisayar-Teknoloji gibi). */
    static String driveOnerisi(String bolum, String fakulte) {
        String b = ilkKelime(bolum);
        String f = ilkKelime(fakulte);
        return b.isEmpty() ? "" : f.isEmpty() ? b : b + "-" + f;
    }

    private static String ilkKelime(String s) {
        return s.strip().split("[\\s-]+")[0];
    }

    private static String asciiKucuk(String s) {
        String k = s.replace("ı", "i").replace("İ", "i").toLowerCase(Locale.forLanguageTag("tr"));
        return Normalizer.normalize(k, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]", "");
    }

    private static List<String> satirlar(String metin) {
        return Arrays.stream(metin.split("\n")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
    }
}
