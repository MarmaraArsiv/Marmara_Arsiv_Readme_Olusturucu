package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ana menü. Araç Marmara'nın tüm bölüm arşivleri için ortaktır: açılışta son kullanılan arşivi (yoksa aracın
 * yanındaki klasörlerde bulduğu arşivi) açar; menüden başka bir arşive geçilebilir ya da yeni bölüm arşivi kurulabilir.
 */
public class MainApp extends Application {

    public static final String VERSION = "0.2.0";

    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Path depo = ReadmeGenerator.jsonKlasoruBul(AppConfig.jsonDeposu());
        if (depo == null) {
            List<Path> komsular = ReadmeGenerator.komsuArsivler(Path.of(".."));
            depo = komsular.size() == 1 ? komsular.get(0) : arsivSec(komsular);
        }
        if (depo == null) {
            Platform.exit();
            return;
        }
        menuyuGoster(depo);
    }

    private void menuyuGoster(Path depo) {
        AppConfig.jsonDeposunuAyarla(depo);
        String baslik = arsivBasligi(depo);
        stage.setTitle("Marmara Arşiv Düzenleyici — " + baslik);

        Label arsivLabel = new Label(baslik);
        arsivLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        Label depoLabel = new Label(ReadmeGenerator.repoKokuBul(depo).toString());
        depoLabel.setWrapText(true);
        Label versionLabel = new Label("Sürüm: " + VERSION);

        Button girisBtn = buton("Giriş Güncelle", "#C0392B", () -> new GirisWindow(depo).show());
        Button repoKullanimiBtn = buton("Repo Kullanımı Düzenle", "#27AE60", () -> new RepoKullanimiWindow(depo).show());
        Button dersBtn = buton("Ders Ekle/Güncelle", "#2980B9", () -> new DersEkleGuncelleWindow(depo).show());
        Button hocaBtn = buton("Hoca Ekle/Güncelle", "#8E44AD", () -> new HocaEkleGuncelleWindow(depo).show());
        Button katkidaBulunanBtn = buton("Katkıda Bulunan Ekle/Güncelle", "#D35400", () -> new KatkidaBulunanWindow(depo).show());
        Button donemBtn = buton("Dönem Ekle/Güncelle", "#16A085", () -> new DonemWindow(depo).show());
        Button konfigurasyonBtn = buton("Konfigürasyon Düzenle", "#9B59B6", () -> new KonfigurasyonWindow(depo).show());
        Button gitBtn = buton("Git İşlemleri", "#2C3E50", () -> new GitIslemleriWindow(depo).show());
        Button baskaBtn = buton("📂 Başka Bölümün Arşivini Aç", "#7F8C8D", () -> {
            Path secilen = arsivSec(new ArrayList<>());
            if (secilen != null) {
                menuyuGoster(secilen);
            }
        });
        Button yeniBtn = buton("🆕 Yeni Bölüm Arşivi Oluştur", "#7F8C8D", () -> {
            Path yeni = new YeniArsivWindow(stage).show();
            if (yeni != null && Dialogs.confirm("Yeni Arşiv", "Yeni arşiv oluşturuldu. Şimdi açılsın mı?")) {
                menuyuGoster(yeni);
            }
        });

        VBox root = new VBox(8, arsivLabel, depoLabel, versionLabel, girisBtn, repoKullanimiBtn, dersBtn, hocaBtn,
                katkidaBulunanBtn, donemBtn, konfigurasyonBtn, gitBtn, new Separator(), baskaBtn, yeniBtn);
        root.setPadding(new Insets(14));

        stage.setScene(new Scene(root, 460, 560));
        stage.show();
    }

    /** giris.json'daki başlık (ör. "Marmara Bilgisayar Mühendisliği Arşiv"); okunamazsa klasör adı. */
    static String arsivBasligi(Path depo) {
        try {
            GirisBolumu g = JsonUtil.read(depo.resolve(ReadmeGenerator.GIRIS_JSON), GirisBolumu.class);
            if (g != null && !g.baslik.isBlank()) {
                return g.baslik;
            }
        } catch (RuntimeException ignored) {
            // bozuk giris.json: başlık yerine klasör adı
        }
        return ReadmeGenerator.repoKokuBul(depo).getFileName().toString();
    }

    /**
     * Açılacak arşivi kullanıcıya sorar: son açılanlar, verilen adaylar, klasör seçme ya da yeni arşiv kurma.
     * Vazgeçilirse null.
     */
    private Path arsivSec(List<Path> adaylar) {
        Map<String, Path> secenekler = new LinkedHashMap<>();
        List<Path> hepsi = new ArrayList<>(AppConfig.sonArsivler());
        adaylar.stream().filter(p -> !hepsi.contains(p)).forEach(hepsi::add);
        for (Path p : hepsi) {
            secenekler.put(arsivBasligi(p) + "  (" + ReadmeGenerator.repoKokuBul(p) + ")", p);
        }
        String klasorSec = "📁 Klasör seç…";
        String yeni = "🆕 Yeni bölüm arşivi oluştur…";
        List<String> metinler = new ArrayList<>(secenekler.keySet());
        metinler.add(klasorSec);
        metinler.add(yeni);
        while (true) {
            String secim = Dialogs.choice("Arşiv Seç", "Hangi bölümün arşivi açılsın?", metinler, metinler.get(0))
                    .orElse(null);
            if (secim == null) {
                return null;
            }
            if (secenekler.containsKey(secim)) {
                return secenekler.get(secim);
            }
            if (secim.equals(yeni)) {
                Path olusan = new YeniArsivWindow(stage).show();
                if (olusan != null) {
                    return olusan;
                }
                continue;
            }
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Arşiv reposunun klasörünü seçin");
            File secilen = chooser.showDialog(stage);
            if (secilen == null) {
                continue;
            }
            Path json = ReadmeGenerator.jsonKlasoruBul(secilen.toPath());
            if (json != null) {
                return json;
            }
            Dialogs.error("Bulunamadı", secilen + " içinde .github/json_dosyalari klasörü yok. Arşiv reposunun "
                    + "klasörünü seçin ya da yeni bölüm arşivi oluşturun.");
        }
    }

    private Button buton(String metin, String renk, Runnable aksiyon) {
        Button b = new Button(metin);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle("-fx-background-color: " + renk + "; -fx-text-fill: white;");
        b.setOnAction(e -> {
            try {
                aksiyon.run();
            } catch (Exception ex) {
                Dialogs.error("Hata", "Bir hata oluştu: " + ex.getMessage());
            }
        });
        return b;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
