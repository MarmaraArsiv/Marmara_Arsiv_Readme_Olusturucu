package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

/** Python main.py'nin (App sınıfının) Java/JavaFX portu — ana menü penceresi. */
public class MainApp extends Application {

    public static final String VERSION = "0.1.0";

    @Override
    public void start(Stage primaryStage) {
        // Kayıtlı yol (eski json_dosyalari yeri dahil) ya da aracın yanındaki arşiv; bulunamazsa kullanıcıya sorulur.
        Path jsonDeposu = ReadmeGenerator.jsonKlasoruBul(AppConfig.jsonDeposu());
        if (jsonDeposu == null) {
            jsonDeposu = ReadmeGenerator.jsonKlasoruBul(Path.of("..", "Marmara_Bilgisayar_Muhendisligi_Arsiv"));
        }
        while (jsonDeposu == null) {
            Path secilen = jsonDepoSecimiIste(primaryStage);
            if (secilen == null) {
                return;
            }
            jsonDeposu = ReadmeGenerator.jsonKlasoruBul(secilen);
            if (jsonDeposu == null) {
                Dialogs.error("Bulunamadı", secilen + " içinde .github/json_dosyalari klasörü yok. "
                        + "Arşiv reposunun klasörünü seçin.");
            }
        }
        AppConfig.jsonDeposunuAyarla(jsonDeposu);
        final Path depo = jsonDeposu;

        primaryStage.setTitle("Marmara Readme Düzenleyici");

        Label versionLabel = new Label("Sürüm: " + VERSION);
        Label depoLabel = new Label("JSON Deposu: " + depo.toAbsolutePath());
        depoLabel.setWrapText(true);

        Button girisBtn = buton("Giriş Güncelle", "#C0392B", () -> new GirisWindow(depo).show());
        Button repoKullanimiBtn = buton("Repo Kullanımı Düzenle", "#27AE60", () -> new RepoKullanimiWindow(depo).show());
        Button dersBtn = buton("Ders Ekle/Güncelle", "#2980B9", () -> new DersEkleGuncelleWindow(depo).show());
        Button hocaBtn = buton("Hoca Ekle/Güncelle", "#8E44AD", () -> new HocaEkleGuncelleWindow(depo).show());
        Button katkidaBulunanBtn = buton("Katkıda Bulunan Ekle/Güncelle", "#D35400", () -> new KatkidaBulunanWindow(depo).show());
        Button donemBtn = buton("Dönem Ekle/Güncelle", "#16A085", () -> new DonemWindow(depo).show());
        Button konfigurasyonBtn = buton("Konfigürasyon Düzenle", "#9B59B6", () -> new KonfigurasyonWindow(depo).show());
        Button gitBtn = buton("Git İşlemleri", "#2C3E50", () -> new GitIslemleriWindow(depo).show());

        VBox root = new VBox(8, versionLabel, depoLabel, girisBtn, repoKullanimiBtn, dersBtn, hocaBtn,
                katkidaBulunanBtn, donemBtn, konfigurasyonBtn, gitBtn);
        root.setPadding(new Insets(14));

        primaryStage.setScene(new Scene(root, 460, 480));
        primaryStage.show();
    }

    private Path jsonDepoSecimiIste(Stage owner) {
        Dialogs.info("Arşiv Klasörünü Seçin", "Arşiv reposunun klasörünü seçin "
                + "(ör. Marmara_Bilgisayar_Muhendisligi_Arsiv). JSON dosyaları .github/json_dosyalari içinde aranır.");
        DirectoryChooser chooser = new DirectoryChooser();
        File secilen = chooser.showDialog(owner);
        return secilen != null ? secilen.toPath() : null;
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
