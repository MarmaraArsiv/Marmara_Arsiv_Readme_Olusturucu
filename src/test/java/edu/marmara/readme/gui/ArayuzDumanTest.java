package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Ekransız (Monocle) duman testi: her pencere ve düzenleme diyaloğu arşiv verisinin bir kopyasıyla
 * açılıp kapatılır, hata fırlatmamalı ve hiçbir JSON'u değiştirmemeli. Arşiv verilmezse atlanır:
 * <pre>./mvnw test -Darsiv.json=../Marmara_Bilgisayar_Muhendisligi_Arsiv</pre>
 */
class ArayuzDumanTest {

    private static ScheduledExecutorService kapatici;

    @BeforeAll
    static void javafxBaslat() throws InterruptedException {
        ArayuzRobotu.baslat();
        // Açılan her pencereyi (uyarı kutuları dahil) kısa süre sonra kapat; showAndWait geri dönsün.
        kapatici = Executors.newSingleThreadScheduledExecutor();
        kapatici.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            for (Window w : new ArrayList<>(Window.getWindows())) {
                if (w instanceof Stage s) {
                    s.close();
                } else {
                    w.hide();
                }
            }
        }), 300, 300, TimeUnit.MILLISECONDS);
    }

    @AfterAll
    static void durdur() throws InterruptedException {
        if (kapatici != null) {
            kapatici.shutdownNow();
            kapatici.awaitTermination(5, TimeUnit.SECONDS);
            ArayuzRobotu.fx(() -> null); // sıradaki kapatma işleri sonraki testlere taşmasın
        }
    }

    /** Arşivin JSON klasörünün geçici bir kopyası (pencereler arşivin kendisine yazamasın). */
    private static Path veriKopyasi(Path hedef) throws IOException {
        String yol = System.getProperty("arsiv.json", "");
        Path kaynak = yol.isEmpty() ? null : ReadmeGenerator.jsonKlasoruBul(Path.of(yol));
        assumeTrue(kaynak != null, "arsiv.json verilmedi, arayüz testi atlandı");
        try (Stream<Path> dosyalar = Files.list(kaynak)) {
            for (Path f : dosyalar.toList()) {
                Files.copy(f, hedef.resolve(f.getFileName()));
            }
        }
        return hedef;
    }

    private static void fxIcindeCalistir(String ad, Runnable is, List<String> hatalar) throws InterruptedException {
        CountDownLatch bitti = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                is.run();
            } catch (Throwable t) {
                hatalar.add(ad + ": " + t);
            } finally {
                bitti.countDown();
            }
        });
        if (!bitti.await(30, TimeUnit.SECONDS)) {
            hatalar.add(ad + ": 30 saniyede kapanmadı");
        }
    }

    @Test
    void tumPencerelerAcilipKapanir(@TempDir Path gecici) throws Exception {
        Path json = veriKopyasi(gecici);
        List<String> once = new ArrayList<>();
        try (Stream<Path> dosyalar = Files.list(json)) {
            for (Path f : dosyalar.sorted().toList()) {
                once.add(f.getFileName() + "=" + Files.readString(f));
            }
        }

        DerslerBolumu dersler = JsonUtil.read(json.resolve(ReadmeGenerator.DERSLER_JSON), DerslerBolumu.class);
        HocalarBolumu hocalar = JsonUtil.read(json.resolve(ReadmeGenerator.HOCALAR_JSON), HocalarBolumu.class);
        DonemlerBolumu donemler = JsonUtil.read(json.resolve(ReadmeGenerator.DONEMLER_JSON), DonemlerBolumu.class);

        List<String> hatalar = new ArrayList<>();
        fxIcindeCalistir("Giriş", () -> new GirisWindow(json).show(), hatalar);
        fxIcindeCalistir("Repo Kullanımı", () -> new RepoKullanimiWindow(json).show(), hatalar);
        fxIcindeCalistir("Dersler", () -> new DersEkleGuncelleWindow(json).show(), hatalar);
        fxIcindeCalistir("Hocalar", () -> new HocaEkleGuncelleWindow(json).show(), hatalar);
        fxIcindeCalistir("Katkıda Bulunanlar", () -> new KatkidaBulunanWindow(json).show(), hatalar);
        fxIcindeCalistir("Dönemler", () -> new DonemWindow(json).show(), hatalar);
        fxIcindeCalistir("Konfigürasyon", () -> new KonfigurasyonWindow(json).show(), hatalar);
        fxIcindeCalistir("Hoca/Ders Adları", () -> new HocaDersAdlariWindow(json).show(), hatalar);
        fxIcindeCalistir("Git İşlemleri", () -> new GitIslemleriWindow(json).show(), hatalar);
        fxIcindeCalistir("Ders düzenle (akademisyenli)", () -> new DersDuzenlemeDialog(new Stage(),
                dersler.dersler.stream().filter(d -> !d.akademisyenler.isEmpty()).findFirst().orElseThrow(),
                dersler, hocalar, donemler, () -> { }, d -> { }).show(), hatalar);
        fxIcindeCalistir("Ders ekle", () -> new DersDuzenlemeDialog(new Stage(), null, dersler, hocalar, donemler,
                () -> { }, d -> { }).show(), hatalar);
        fxIcindeCalistir("Yıl düzenle", () -> new AkademisyenYiliDialog(new Stage(),
                dersler.dersler.get(0).akademisyenler.get(0), null, hocalar,
                dersler.dersler.get(0).akademisyenler).show(), hatalar);
        fxIcindeCalistir("Hoca düzenle", () -> new HocaDuzenlemeDialog(new Stage(), hocalar.hocalar.get(0), hocalar,
                dersler, json.resolve(ReadmeGenerator.DERSLER_JSON), () -> { }, h -> { }).show(), hatalar);
        fxIcindeCalistir("Hoca ekle", () -> new HocaDuzenlemeDialog(new Stage(), null, hocalar, dersler,
                json.resolve(ReadmeGenerator.DERSLER_JSON), () -> { }, h -> { }).show(), hatalar);

        assertEquals(List.of(), hatalar, "Hata veren pencereler");
        List<String> sonra = new ArrayList<>();
        try (Stream<Path> dosyalar = Files.list(json)) {
            for (Path f : dosyalar.sorted().toList()) {
                sonra.add(f.getFileName() + "=" + Files.readString(f));
            }
        }
        assertEquals(once, sonra, "Pencereleri yalnızca açıp kapatmak JSON'ları değiştirmemeli");
    }
}
