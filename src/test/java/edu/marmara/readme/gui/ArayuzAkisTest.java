package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.UrlKodla;
import javafx.application.Platform;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import static edu.marmara.readme.gui.ArayuzRobotu.arkaPlanda;
import static edu.marmara.readme.gui.ArayuzRobotu.bas;
import static edu.marmara.readme.gui.ArayuzRobotu.bekle;
import static edu.marmara.readme.gui.ArayuzRobotu.bul;
import static edu.marmara.readme.gui.ArayuzRobotu.diyalogDugmesi;
import static edu.marmara.readme.gui.ArayuzRobotu.etiketinAltindaki;
import static edu.marmara.readme.gui.ArayuzRobotu.fx;
import static edu.marmara.readme.gui.ArayuzRobotu.kapanmasiniBekle;
import static edu.marmara.readme.gui.ArayuzRobotu.metinGir;
import static edu.marmara.readme.gui.ArayuzRobotu.pencere;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Arayüzü bir kullanıcı gibi kullanan ekransız testler: alanları doldurur, düğmelere basar, diyalogları
 * onaylar; sonra JSON'ları ve üretilen README'leri denetler. Hepsi arşiv verisinin geçici kopyasıyla
 * çalışır, arşive hiçbir şey yazılmaz. Arşiv verilmezse atlanır:
 * <pre>./mvnw test -Darsiv.json=../Marmara_Bilgisayar_Muhendisligi_Arsiv</pre>
 */
class ArayuzAkisTest {

    private static final String HOCA = "Doç. Dr. Buket Doğan";
    private static final String TERFI = "Prof. Dr. Buket Doğan";

    @BeforeAll
    static void baslat() throws InterruptedException {
        ArayuzRobotu.baslat();
    }

    @AfterEach
    void temizle() {
        ArayuzRobotu.hepsiniKapat();
    }

    // ------------------------------------------------------------ hazırlık

    private static Path arsivJson() {
        String yol = System.getProperty("arsiv.json", "");
        Path json = yol.isEmpty() ? null : ReadmeGenerator.jsonKlasoruBul(Path.of(yol));
        assumeTrue(json != null, "arsiv.json verilmedi, arayüz akış testi atlandı");
        return json;
    }

    /** Arşivin JSON'larını hedef klasöre kopyalar; README'ler readmeKoku'nden okunur. */
    private static Path jsonKopyasi(Path hedef, Path readmeKoku) throws IOException {
        Path kaynak = arsivJson();
        Files.createDirectories(hedef);
        try (Stream<Path> dosyalar = Files.list(kaynak)) {
            for (Path f : dosyalar.toList()) {
                Files.copy(f, hedef.resolve(f.getFileName()));
            }
        }
        if (readmeKoku != null) {
            Konfigurasyon k = JsonUtil.read(hedef.resolve(ReadmeGenerator.KONFIGURASYON_JSON), Konfigurasyon.class);
            k.dokumanlar_repo_yolu = readmeKoku.toAbsolutePath().toString();
            JsonUtil.write(hedef.resolve(ReadmeGenerator.KONFIGURASYON_JSON), k);
        }
        return hedef;
    }

    /** Arşivdeki bütün README'ler ve JSON klasörüyle aynı yapıda bir kopya; JSON klasörünü döner. */
    private static Path arsivKopyasi(Path hedef) throws IOException {
        Path json = arsivJson();
        Path kok = ReadmeGenerator.repoKokuBul(json);
        for (Map.Entry<String, String> e : readmeler(kok).entrySet()) {
            Path f = hedef.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.writeString(f, e.getValue());
        }
        return jsonKopyasi(hedef.resolve(kok.relativize(json)), null);
    }

    /** Klasördeki README.md dosyaları (.git hariç): göreli yol → içerik. */
    private static Map<String, String> readmeler(Path kok) throws IOException {
        Map<String, String> sonuc = new TreeMap<>();
        try (Stream<Path> s = Files.walk(kok)) {
            for (Path f : s.filter(p -> p.getFileName().toString().equals("README.md"))
                    .filter(p -> !kok.relativize(p).toString().startsWith(".git" + java.io.File.separator))
                    .toList()) {
                sonuc.put(kok.relativize(f).toString(), Files.readString(f, StandardCharsets.UTF_8));
            }
        }
        return sonuc;
    }

    private static DerslerBolumu dersler(Path json) {
        return JsonUtil.read(json.resolve(ReadmeGenerator.DERSLER_JSON), DerslerBolumu.class);
    }

    private static HocalarBolumu hocalar(Path json) {
        return JsonUtil.read(json.resolve(ReadmeGenerator.HOCALAR_JSON), HocalarBolumu.class);
    }

    private static long kayitSayisi(DerslerBolumu d, String hoca) {
        return d.dersler.stream().flatMap(x -> x.akademisyenler.stream()).flatMap(y -> y.hocalar.stream())
                .filter(h -> h.ad.equals(hoca)).count();
    }

    /** Listede çift tıklama (düzenleme penceresini açar; beklemeden döner). */
    private static void ciftTikla(ListView<?> lv) {
        Platform.runLater(() -> lv.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                MouseButton.PRIMARY, 2, false, false, false, false, false, false, false, false, false, false, null)));
    }

    private static void kapat(Window w, CountDownLatch l) throws InterruptedException {
        fx(() -> ((Stage) w).close());
        bekle(l);
    }

    private static String readmeOf(Map<Path, String> cikti, String yol) {
        return cikti.get(Path.of(yol));
    }

    // ------------------------------------------------------------ ders

    @Test
    void dersEkleYilVeHocalarlaKaydedilir(@TempDir Path gecici) throws Exception {
        Path json = jsonKopyasi(gecici.resolve("json"), ReadmeGenerator.repoKokuBul(arsivJson()));

        CountDownLatch kapandi = arkaPlanda(() -> new DersEkleGuncelleWindow(json).show());
        Window w = pencere("Ders Ekle/Güncelle");
        bas(w, "➕ Ders Ekle");
        Window d = pencere("Ders Ekle");
        String tip = fx(() -> {
            ArayuzRobotu.<TextField>etiketinAltindaki(d, "Ders Adı").setText("Test Dersi");
            ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d, "Dönem / Seçmeli Havuzu").setValue("Mesleki Seçmeli");
            ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d, "Müfredat").setValue("2026 Sonrası");
            ArayuzRobotu.<TextField>etiketinAltindaki(d, "Müfredat Notu (ℹ️ satırı)").setText("Deneme notu.");
            return ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d, "Ders Tipi").getValue();
        });
        assertEquals("Seçmeli", tip, "havuz seçilince tip Seçmeli olmalı");

        bas(d, "Yıl Ekle");
        Window y = pencere("Yıl Ekle");
        assertEquals("2026-2027", fx(() -> ArayuzRobotu.<TextField>etiketinAltindaki(y, "Akademik Yıl").getText()));
        bas(y, "Listeden Hoca Ekle");
        Window sec = pencere("Hoca Seç");
        fx(() -> bul(sec, ComboBox.class).setValue(HOCA));
        diyalogDugmesi(sec, ButtonType.OK);
        fx(() -> bul(y, ListView.class).getSelectionModel().select(0));
        bas(y, "Not Ekle/Değiştir");
        metinGir("Not", "lab");
        bas(y, "Listede Olmayan İsim");
        metinGir("İsim", "Listede Olmayan Hoca");
        bas(y, "Tamam");
        kapanmasiniBekle(y);

        bas(d, "Ekle");
        kapanmasiniBekle(d);
        kapat(w, kapandi);

        DerslerBolumu sonra = dersler(json);
        DerslerBolumu once = dersler(arsivJson());
        assertEquals(once.dersler.size() + 1, sonra.dersler.size());
        int sonMesleki = 0;
        for (int i = 0; i < once.dersler.size(); i++) {
            if (once.dersler.get(i).ustKlasor().equals("Mesleki Seçmeli")) {
                sonMesleki = i;
            }
        }
        Ders yeni = sonra.dersler.get(sonMesleki + 1);
        assertEquals("Mesleki Seçmeli/Test Dersi", yeni.klasor, "aynı havuzun son dersinin arkasına eklenmeli");
        assertEquals("Seçmeli", yeni.tip);
        assertEquals("2026 Sonrası", yeni.mufredat);
        assertEquals("Deneme notu.", yeni.mufredat_notu);
        assertEquals(1, yeni.akademisyenler.size());
        AkademisyenYili yil = yeni.akademisyenler.get(0);
        assertEquals("2026-2027", yil.yil);
        assertEquals(List.of(HOCA + " (lab)", "Listede Olmayan Hoca"),
                yil.hocalar.stream().map(h -> h.not.isEmpty() ? h.ad : h.ad + " (" + h.not + ")").toList());

        Map<Path, String> cikti = new ReadmeGenerator(json, null).uret();
        assertTrue(readmeOf(cikti, "Mesleki Seçmeli/Test Dersi/README.md").contains(
                "- **2026-2027**:\n  - **BD** — Doç. Dr. Buket Doğan (lab)\n  - Listede Olmayan Hoca\n"));
        assertTrue(readmeOf(cikti, "README.md").contains("  - 📖 [Test Dersi](#-test-dersi) (2026-2027)\n"),
                "hoca kartında yeni ders");
    }

    @Test
    void dersDuzenlenirSilinirVeCtrlZIleGeriGelir(@TempDir Path gecici) throws Exception {
        Path json = jsonKopyasi(gecici.resolve("json"), ReadmeGenerator.repoKokuBul(arsivJson()));
        String ilkKlasor = dersler(json).dersler.get(0).klasor;

        CountDownLatch kapandi = arkaPlanda(() -> new DersEkleGuncelleWindow(json).show());
        Window w = pencere("Ders Ekle/Güncelle");
        @SuppressWarnings("unchecked")
        ListView<Ders> lv = fx(() -> bul(w, ListView.class, x -> !x.getItems().isEmpty() && x.getItems().get(0) instanceof Ders));

        // Düzenle: müfredat notunu değiştir.
        fx(() -> lv.getSelectionModel().select(0));
        ciftTikla(lv);
        Window d = pencere("Ders Düzenle");
        fx(() -> ArayuzRobotu.<TextField>etiketinAltindaki(d, "Müfredat Notu (ℹ️ satırı)").setText("Yeni not."));
        bas(d, "Değişiklikleri Kaydet");
        kapanmasiniBekle(d);
        DerslerBolumu duzenlenmis = dersler(json);
        assertEquals("Yeni not.", duzenlenmis.dersler.get(0).mufredat_notu);
        assertEquals(ilkKlasor, duzenlenmis.dersler.get(0).klasor);
        String duzenlenmisMetin = Files.readString(json.resolve(ReadmeGenerator.DERSLER_JSON));

        // Sil (onay kutusunda Evet).
        fx(() -> lv.getSelectionModel().select(0));
        ciftTikla(lv);
        Window d2 = pencere("Ders Düzenle");
        bas(d2, "Dersi Sil");
        diyalogDugmesi(pencere("Onay"), ButtonType.YES);
        kapanmasiniBekle(d2);
        assertTrue(dersler(json).dersler.stream().noneMatch(x -> x.klasor.equals(ilkKlasor)), "ders silinmeli");

        // Ctrl+Z: aynı yere geri gelir, dosya silmeden önceki hâline döner.
        fx(() -> w.getScene().getOnKeyPressed().handle(
                new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.Z, false, true, false, false)));
        assertEquals(duzenlenmisMetin, Files.readString(json.resolve(ReadmeGenerator.DERSLER_JSON)));
        kapat(w, kapandi);
    }

    // ------------------------------------------------------------ hoca

    @Test
    void hocaEklenirVeTerfiEdenHocaninYorumlariKorunur(@TempDir Path gecici) throws Exception {
        // README kopyasında hocanın kartına onaylı bir yorum ekle (GitHub betiğinin yazdığı biçimde).
        Path kok = ReadmeGenerator.repoKokuBul(arsivJson());
        Path readmeKoku = Files.createDirectories(gecici.resolve("arsiv"));
        String kart = "#### 👩‍🏫 " + HOCA + " \n";
        String anaReadme = Files.readString(kok.resolve("README.md"));
        int i = anaReadme.indexOf(kart);
        assumeTrue(i >= 0, HOCA + " kartı bulunamadı");
        String gorus = "- 💬 **Öğrenci Görüşleri:**\n";
        int j = anaReadme.indexOf(gorus, i) + gorus.length();
        Files.writeString(readmeKoku.resolve("README.md"), anaReadme.substring(0, j)
                + "  - 👤 **_Deneme_**: Harika hoca ℹ️ Yorum **10.2026** tarihinde yapılmıştır.\n" + anaReadme.substring(j));
        Path json = jsonKopyasi(gecici.resolve("json"), readmeKoku);
        long kayit = kayitSayisi(dersler(json), HOCA);
        assertTrue(kayit > 0);

        CountDownLatch kapandi = arkaPlanda(() -> new HocaEkleGuncelleWindow(json).show());
        Window w = pencere("Hocaları Ekle/Güncelle");

        // Yeni hoca: kısaltma ve grup ünvan/addan önerilir.
        bas(w, "➕ Hoca Ekle");
        Window d = pencere("Hoca Ekle");
        String[] oneriler = fx(() -> {
            ArayuzRobotu.<TextField>etiketinAltindaki(d, "Ad Soyad").setText("Deneme Yılmaz");
            ArayuzRobotu.<TextField>etiketinAltindaki(d, "Ofis").setText("T4-999");
            ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d, "Cinsiyet (kart emojisi)").setValue("Erkek (👨‍🏫)");
            return new String[]{
                    ArayuzRobotu.<TextField>etiketinAltindaki(d, "Kısaltma (ders kartlarında hocaya link olarak görünür)")
                            .getText(),
                    ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d, "Ana README'deki grup").getValue()};
        });
        assertEquals("Doktor Öğretim Üyeleri", oneriler[1]);
        assertFalse(oneriler[0].isEmpty(), "kısaltma önerilmeli");
        assertTrue(hocalar(json).hocalar.stream().noneMatch(h -> h.kisaltma.equals(oneriler[0])),
                "önerilen kısaltma başka hocada olmamalı");
        bas(d, "Ekle");
        kapanmasiniBekle(d);

        // Terfi: Doç. Dr. → Prof. Dr.; grup Doçentler → Profesörler olmalı, derslerdeki ad da değişmeli.
        @SuppressWarnings("unchecked")
        ListView<Hoca> lv = fx(() -> bul(w, ListView.class, x -> !x.getItems().isEmpty() && x.getItems().get(0) instanceof Hoca));
        fx(() -> lv.getSelectionModel().select(lv.getItems().stream().filter(h -> h.ad.equals(HOCA)).findFirst()
                .orElseThrow()));
        ciftTikla(lv);
        Window d2 = pencere("Hoca Düzenle");
        String grup = fx(() -> {
            ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d2, "Ünvan").setValue("Prof. Dr.");
            return ArayuzRobotu.<ComboBox<String>>etiketinAltindaki(d2, "Ana README'deki grup").getValue();
        });
        assertEquals("Profesörler", grup);
        bas(d2, "Değişiklikleri Kaydet");
        kapanmasiniBekle(d2);
        kapat(w, kapandi);

        HocalarBolumu h = hocalar(json);
        Hoca yeni = h.hocalar.stream().filter(x -> x.ad.equals("Dr. Öğretim Üyesi Deneme Yılmaz")).findFirst()
                .orElseThrow();
        assertEquals(oneriler[0], yeni.kisaltma);
        assertEquals("T4-999", yeni.ofis);
        assertEquals(Boolean.TRUE, yeni.erkek_mi);
        Hoca terfi = h.hocalar.stream().filter(x -> x.ad.equals(TERFI)).findFirst().orElseThrow();
        assertEquals("Profesörler", terfi.grup);
        assertEquals(0, kayitSayisi(dersler(json), HOCA));
        assertEquals(kayit, kayitSayisi(dersler(json), TERFI));

        Map<Path, String> cikti = new ReadmeGenerator(json, null).uret();
        cikti.forEach((yol, metin) -> assertFalse(metin.contains(HOCA), yol + " eski adı içeriyor"));
        String kok2 = readmeOf(cikti, "README.md");
        int k = kok2.indexOf("#### 👩‍🏫 " + TERFI + " \n");
        assertTrue(k >= 0, "terfi eden hocanın kartı");
        String yeniKart = kok2.substring(k, kok2.indexOf("- 📚 **Verdiği Dersler:**", k));
        assertTrue(yeniKart.contains("  - 👤 **_Deneme_**: Harika hoca"), "onaylı yorum yeni kartta korunmalı");
        assertTrue(yeniKart.contains("=" + UrlKodla.kodla(TERFI) + ")"), "yorum linki yeni adla");
        assertTrue(kok2.indexOf("#### 👨‍🏫 Dr. Öğretim Üyesi Deneme Yılmaz \n") > 0, "yeni hocanın kartı");
        assertTrue(cikti.values().stream().anyMatch(m -> m.contains("- **[" + TERFI + "](../../README.md#")
                        && m.contains("    - 👤 **_Deneme_**: Harika hoca")),
                "ders sayfasındaki hoca bölümünde yorum yeni adla görünmeli");
    }

    // ------------------------------------------------------------ git işlemleri

    @Test
    void readmeGuncelleDugmesiYalnizcaDegisenleriYazarVeHataliVeriyeDokunmaz(@TempDir Path gecici) throws Exception {
        Path json = arsivKopyasi(gecici.resolve("arsiv"));
        Path kok = gecici.resolve("arsiv");
        Map<String, String> ilk = readmeler(kok);

        CountDownLatch kapandi = arkaPlanda(() -> new GitIslemleriWindow(json).show());
        Window w = pencere("Git İşlemleri");

        // Veri değişmediyse hiçbir README değişmez.
        bas(w, "📝 Readme Güncelle");
        fx(() -> null);
        assertEquals(ilk, readmeler(kok));

        // Bir derse yeni yıl: o dersin README'si ve özet sayfaları değişir, sonuç JSON ile uyumlu.
        DerslerBolumu d = dersler(json);
        Ders ders = d.dersler.stream().filter(x -> !x.akademisyenler.isEmpty() && x.mufredat.equals("Her İki Müfredat"))
                .findFirst().orElseThrow();
        AkademisyenYili yil = new AkademisyenYili();
        yil.yil = "2030-2031";
        yil.hocalar.add(new DersHocasi(HOCA, ""));
        ders.akademisyenler.add(0, yil);
        JsonUtil.write(json.resolve(ReadmeGenerator.DERSLER_JSON), d);
        bas(w, "📝 Readme Güncelle");
        fx(() -> null);
        Map<String, String> sonra = readmeler(kok);
        assertNotEquals(ilk.get(ders.klasor + "/README.md"), sonra.get(ders.klasor + "/README.md"));
        assertTrue(sonra.get(ders.klasor + "/README.md").contains("- **2030-2031**:\n  - **BD** — " + HOCA + "\n"));
        assertEquals(List.of(), new ReadmeGenerator(json, null).farkliOlanlar());

        // Hatalı veri (aynı klasör iki kez): hata gösterilir, hiçbir dosyaya dokunulmaz.
        d.dersler.add(d.dersler.get(1));
        JsonUtil.write(json.resolve(ReadmeGenerator.DERSLER_JSON), d);
        bas(w, "📝 Readme Güncelle");
        diyalogDugmesi(pencere("Hata"), ButtonType.OK);
        assertEquals(sonra, readmeler(kok));
        assertThrows(IllegalStateException.class, () -> new ReadmeGenerator(json, null).uret());
        kapat(w, kapandi);
    }

    private static String git(Path dizin, String... args) throws IOException, InterruptedException {
        List<String> komut = new ArrayList<>(List.of("git", "-C", dizin.toString()));
        komut.addAll(List.of(args));
        Process p = new ProcessBuilder(komut).redirectErrorStream(true).start();
        String cikti = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, p.waitFor(), String.join(" ", args) + ": " + cikti);
        return cikti;
    }

    private static void yaz(Path f, String metin) throws IOException {
        Files.createDirectories(f.getParent());
        Files.writeString(f, metin);
    }

    @Test
    void pushSeciliDosyalariBotCommitlerininUstuneGonderir(@TempDir Path gecici) throws Exception {
        // Yerel "GitHub": çıplak repo; kullanıcının kopyası ve arada commit atan bir bot kopyası.
        Path is = gecici.resolve("is");
        Files.createDirectories(is);
        git(is, "init", "-q", "-b", "main");
        git(is, "config", "user.name", "Test");
        git(is, "config", "user.email", "test@example.com");
        git(is, "config", "commit.gpgsign", "false");
        Konfigurasyon k = new Konfigurasyon();
        k.dokumanlar_repo_yolu = "../..";
        Files.createDirectories(is.resolve(".github/json_dosyalari"));
        JsonUtil.write(is.resolve(".github/json_dosyalari/" + ReadmeGenerator.KONFIGURASYON_JSON), k);
        yaz(is.resolve("README.md"), "ana\n");
        yaz(is.resolve("1-1/Türkçe 1/README.md"), "türkçe\n");
        yaz(is.resolve("1-1/Fizik 1/README.md"), "fizik\n");
        git(is, "add", "-A");
        git(is, "commit", "-q", "-m", "ilk");
        Path uzak = gecici.resolve("uzak.git");
        git(gecici, "clone", "-q", "--bare", is.toString(), uzak.toString());
        git(is, "remote", "add", "origin", uzak.toString());
        git(is, "fetch", "-q", "origin");
        git(is, "branch", "-q", "-u", "origin/main");

        Path bot = gecici.resolve("bot");
        git(gecici, "clone", "-q", uzak.toString(), bot.toString());
        git(bot, "config", "user.name", "Bot");
        git(bot, "config", "user.email", "bot@example.com");
        git(bot, "config", "commit.gpgsign", "false");
        yaz(bot.resolve("2-1/Bot Dersi/README.md"), "bot\n");
        git(bot, "add", "-A");
        git(bot, "commit", "-q", "-m", "README otomatik güncellendi");
        git(bot, "push", "-q");

        yaz(is.resolve("1-1/Türkçe 1/README.md"), "türkçe 2\n");
        yaz(is.resolve("1-1/Fizik 1/README.md"), "fizik 2\n");
        yaz(is.resolve("Mesleki Seçmeli/Yeni Ders/README.md"), "yeni\n");
        yaz(is.resolve("README.md"), "ana (commit'lenmeyecek)\n");

        Path json = is.resolve(".github/json_dosyalari");
        CountDownLatch kapandi = arkaPlanda(() -> new GitIslemleriWindow(json).show());
        Window w = pencere("Git İşlemleri");
        bas(w, "⬆️ Değişiklikleri GitHub'a Pushla");
        Window p = pencere("Değişiklikleri Pushlama");
        @SuppressWarnings("unchecked")
        ListView<CheckBox> lv = fx(() -> bul(p, ListView.class));
        List<String> listelenen = fx(() -> lv.getItems().stream().map(CheckBox::getText).sorted().toList());
        assertEquals(List.of(" M  1-1/Fizik 1/README.md", " M  1-1/Türkçe 1/README.md", " M  README.md",
                "??  Mesleki Seçmeli/Yeni Ders/README.md"), listelenen);
        fx(() -> {
            lv.getItems().stream().filter(c -> c.getText().equals(" M  README.md")).forEach(c -> c.setSelected(false));
            bul(p, TextArea.class).setText("Test: seçili README'ler");
        });
        bas(p, "OK");
        diyalogDugmesi(pencere("Emin Misiniz?"), ButtonType.YES);
        kapanmasiniBekle(p);
        kapat(w, kapandi);

        assertEquals("Test: seçili README'ler\nREADME otomatik güncellendi\nilk\n",
                git(uzak, "log", "--format=%s", "main"));
        assertEquals("türkçe 2\n", git(uzak, "show", "main:1-1/Türkçe 1/README.md"));
        assertEquals("fizik 2\n", git(uzak, "show", "main:1-1/Fizik 1/README.md"));
        assertEquals("yeni\n", git(uzak, "show", "main:Mesleki Seçmeli/Yeni Ders/README.md"));
        assertEquals("ana\n", git(uzak, "show", "main:README.md"), "işaretlenmeyen dosya gönderilmemeli");
        assertEquals("ana (commit'lenmeyecek)\n", Files.readString(is.resolve("README.md")),
                "işaretlenmeyen değişiklik yerelde kalmalı");
        assertEquals("bot\n", Files.readString(is.resolve("2-1/Bot Dersi/README.md")), "botun commit'i yerele gelmeli");
    }
}
