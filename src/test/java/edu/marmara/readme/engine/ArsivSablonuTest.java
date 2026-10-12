package edu.marmara.readme.engine;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.MufredatAyari;
import edu.marmara.readme.engine.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Başka bölümler için arşiv kurulumu: şablondan yeni arşiv, formları kurulmamış arşivde README'ler, bölüme
 * göre müfredat kuralı ve mevcut arşivin GitHub betiklerinin şablonla aynı olması.
 */
class ArsivSablonuTest {

    private static ArsivSablonu.BolumBilgisi elektrik(MufredatAyari m) {
        return new ArsivSablonu.BolumBilgisi("Elektrik-Elektronik Mühendisliği", "Teknoloji Fakültesi",
                "https://github.com/MarmaraArsiv/tek_elektrik", 4, List.of("Mesleki Seçmeli", "Üniversite Seçmeli"),
                m, "Elektrik-Teknoloji");
    }

    private static Map<String, String> dosyalar(Path kok) throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        try (Stream<Path> s = Files.walk(kok)) {
            for (Path p : s.filter(Files::isRegularFile).filter(p -> !p.toString().contains("/.git/")).sorted().toList()) {
                m.put(kok.relativize(p).toString(), Files.readString(p, StandardCharsets.UTF_8));
            }
        }
        return m;
    }

    @Test
    void yeniBolumArsiviSablondanKurulur(@TempDir Path gecici) throws Exception {
        Path kok = gecici.resolve("tek_elektrik");
        Path json = ArsivSablonu.yeniArsivOlustur(kok, elektrik(new MufredatAyari()), s -> { });

        assertEquals(kok.resolve(".github/json_dosyalari").toAbsolutePath().normalize(), json);
        Map<String, String> f = dosyalar(kok);
        for (String klasor : List.of("1-1", "1-2", "2-1", "2-2", "3-1", "3-2", "4-1", "4-2", "Mesleki Seçmeli", "Üniversite Seçmeli")) {
            assertTrue(f.containsKey(klasor + "/README.md"), klasor + " README'si yok");
        }
        assertFalse(f.containsKey("5-1/README.md"));
        for (String gerekli : List.of("README.md", "CONTRIBUTING.md", "LICENSE", ".gitignore", ".gitattributes",
                ".github/STANDARTLAR.md", ".github/workflows/form_yorum_takibi.yml",
                ".github/workflows/readme_otomatik_guncelleme.yml", ".github/scripts/form_linkleri.py",
                ".github/google_form/form_olustur.gs", ".github/google_form/telegram_bot.gs",
                ".github/google_form/KURULUM.md")) {
            assertTrue(f.containsKey(gerekli), gerekli + " yok");
        }
        // Yer tutucuların hepsi doldu; GitHub'ın ${{ … }} ifadeleri olduğu gibi kaldı.
        f.forEach((yol, metin) -> assertFalse(metin.replace("${{", "").contains("{{"), yol + " içinde yer tutucu kaldı"));
        assertTrue(f.get(".github/workflows/form_yorum_takibi.yml").contains("${{ github.token }}"));
        // Ortak form betiği bütün bölümleri listeler; onun dışında Bilgisayar'a özel metin olmamalı.
        f.forEach((yol, metin) -> assertFalse(!yol.endsWith("form_olustur.gs")
                        && metin.contains("Bilgisayar") && !metin.contains("YTU Bilgisayar"),
                yol + " içinde Bilgisayar Mühendisliği'ne özel metin var"));

        String readme = f.get("README.md");
        assertTrue(readme.startsWith("# 📖 Marmara Elektrik-Elektronik Mühendisliği Arşiv\n"));
        assertTrue(readme.contains("Marmara Üniversitesi Teknoloji Fakültesi Elektrik-Elektronik Mühendisliği öğrencileri"));
        assertTrue(readme.contains("**Elektrik-Teknoloji** klasöründe"));
        assertTrue(readme.contains("- ℹ️ Geri bildirim formları yakında eklenecek."));
        assertTrue(readme.contains("star-history.com/#MarmaraArsiv/tek_elektrik&Date"));
        assertTrue(f.get("CONTRIBUTING.md").contains("Seçmeli dersler `Mesleki Seçmeli/` ve `Üniversite Seçmeli/` altında."));
        assertTrue(f.get("CONTRIBUTING.md").contains("git clone git@github.com:<kullanıcı-adın>/tek_elektrik.git"));
        // Formlar ve bot bütün bölümler için tek merkezden: betikler her arşivde aynı, token koda yazılmaz.
        assertTrue(f.get(".github/google_form/form_olustur.gs").contains("const ARSIVLER = {"));
        assertTrue(f.get(".github/google_form/telegram_bot.gs").contains("ozellik_('TG_TOKEN')"));

        DonemlerBolumu d = JsonUtil.read(json.resolve(ReadmeGenerator.DONEMLER_JSON), DonemlerBolumu.class);
        assertEquals("4. Sınıf Bahar Dönemi", d.donemler.get(7).donem_adi);
        Konfigurasyon k = JsonUtil.read(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), Konfigurasyon.class);
        assertEquals("../..", k.dokumanlar_repo_yolu);
        assertEquals(kok.toAbsolutePath().normalize(), ReadmeGenerator.repoKokuBul(json));

        // Kendisiyle uyumlu: yeniden üretmek hiçbir şeyi değiştirmez, altyapı şablonla aynı.
        assertEquals(List.of(), new ReadmeGenerator(json, null).farkliOlanlar());
        assertEquals(List.of(), ArsivSablonu.altyapiyiGuncelle(kok, false));
        // Aynı klasöre ikinci kez kurulmaz.
        assertThrows(IllegalArgumentException.class,
                () -> ArsivSablonu.yeniArsivOlustur(kok, elektrik(new MufredatAyari()), s -> { }));
    }

    @Test
    void formlarKurulmadanDersEklenirFormlarKurulunceLinklerGelir(@TempDir Path gecici) throws Exception {
        MufredatAyari tek = new MufredatAyari();
        tek.secenekler = new ArrayList<>(List.of("Güncel Müfredat"));
        tek.eski_mufredat_oneki = "";
        Path json = ArsivSablonu.yeniArsivOlustur(gecici.resolve("a"), elektrik(tek), s -> { });

        DerslerBolumu dersler = JsonUtil.read(json.resolve(ReadmeGenerator.DERSLER_JSON), DerslerBolumu.class);
        Ders ders = new Ders();
        ders.ad = "Devre Analizi";
        ders.klasor = "1-2/Devre Analizi";
        ders.tip = "Zorunlu";
        ders.mufredat = "Güncel Müfredat";
        AkademisyenYili y = new AkademisyenYili();
        y.yil = "2027-2028";
        y.hocalar.add(new DersHocasi("Dr. Öğretim Üyesi Deneme Hoca", ""));
        ders.akademisyenler.add(y);
        dersler.dersler.add(ders);
        JsonUtil.write(json.resolve(ReadmeGenerator.DERSLER_JSON), dersler);
        HocalarBolumu hocalar = JsonUtil.read(json.resolve(ReadmeGenerator.HOCALAR_JSON), HocalarBolumu.class);
        Hoca hoca = new Hoca();
        hoca.ad = "Dr. Öğretim Üyesi Deneme Hoca";
        hoca.kisaltma = "DH";
        hoca.grup = "Doktor Öğretim Üyeleri";
        hocalar.hocalar.add(hoca);
        JsonUtil.write(json.resolve(ReadmeGenerator.HOCALAR_JSON), hocalar);

        Map<Path, String> cikti = new ReadmeGenerator(json, null).uret();
        assertTrue(cikti.get(Path.of("README.md")).contains("#### 🧑‍🏫 Dr. Öğretim Üyesi Deneme Hoca \n"
                + "- 🚪 **Ofis:** \n- 🔗 **Araştırma Sayfası:** \n- 💬 **Öğrenci Görüşleri:**\n  - ℹ️ Henüz yorum yok.\n"),
                cikti.get(Path.of("README.md")));
        String dersReadme = cikti.get(Path.of("1-2/Devre Analizi/README.md"));
        assertTrue(dersReadme.contains("- ⭐ **Yıldız Sayıları:**\n    - ℹ️ Henüz yıldız veren yok.\n"), dersReadme);
        assertTrue(dersReadme.contains("- 💬 **Öğrenci Görüşleri:**\n    - ℹ️ Henüz yorum yok.\n"), dersReadme);
        assertTrue(dersReadme.contains("- **2027-2028**:\n"), "Müfredat geçişi olmayan bölümde yıl notu yazılmamalı");
        assertTrue(dersReadme.contains("    - ✍️ Henüz yorum yok.\n"));
        assertFalse(dersReadme.contains("docs.google.com"));

        Konfigurasyon k = JsonUtil.read(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), Konfigurasyon.class);
        k.formlar.put("ders_oylama", new LinkedHashMap<>(Map.of("link", "https://forms.example/oy/viewform",
                "ders_alani", "11", "kolaylik_alani", "12", "gereklilik_alani", "13")));
        JsonUtil.write(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), k);
        dersReadme = new ReadmeGenerator(json, null).uret().get(Path.of("1-2/Devre Analizi/README.md"));
        assertTrue(dersReadme.contains("Henüz yıldız veren yok. Siz de [linkten](https://forms.example/oy/viewform"
                + "?usp=pp_url&entry.11=Devre%20Analizi) anonim şekilde oylamaya katılabilirsiniz."), dersReadme);

        // Link girilip soru kimliği unutulursa README'lere dokunulmaz.
        k.formlar.put("hoca_yorumlama", new LinkedHashMap<>(Map.of("link", "https://forms.example/hoca/viewform")));
        JsonUtil.write(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), k);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> new ReadmeGenerator(json, null).uret());
        assertTrue(e.getMessage().contains("formlar.hoca_yorumlama.hoca_alani boş"), e.getMessage());
    }

    @Test
    void eskiMufredatNotuBolumeGoreAyarlanir(@TempDir Path gecici) throws Exception {
        MufredatAyari m = new MufredatAyari();
        m.secenekler = new ArrayList<>(List.of("2028 Öncesi", "2028 Sonrası"));
        m.eski_mufredat_oneki = "2028 Öncesi";
        m.eski_mufredat_notu_yili = "2028-2029";
        m.eski_mufredat_notu = "yalnızca eski müfredattakiler alabilir";
        Path json = ArsivSablonu.yeniArsivOlustur(gecici.resolve("a"), elektrik(m), s -> { });
        DerslerBolumu dersler = JsonUtil.read(json.resolve(ReadmeGenerator.DERSLER_JSON), DerslerBolumu.class);
        Ders ders = new Ders();
        ders.ad = "Eski Ders";
        ders.klasor = "3-1/Eski Ders";
        ders.tip = "Zorunlu";
        ders.mufredat = "2028 Öncesi";
        for (String yil : List.of("2028-2029", "2027-2028")) {
            AkademisyenYili y = new AkademisyenYili();
            y.yil = yil;
            y.hocalar.add(new DersHocasi("Prof. Dr. Deneme Hoca", ""));
            ders.akademisyenler.add(y);
        }
        dersler.dersler.add(ders);
        JsonUtil.write(json.resolve(ReadmeGenerator.DERSLER_JSON), dersler);

        String r = new ReadmeGenerator(json, null).uret().get(Path.of("3-1/Eski Ders/README.md"));
        assertTrue(r.contains("- **2028-2029** (yalnızca eski müfredattakiler alabilir):\n"), r);
        assertTrue(r.contains("- **2027-2028**:\n"), r);
    }

    @Test
    void bilgiHatalariGosterilir() {
        MufredatAyari bos = new MufredatAyari();
        bos.secenekler = new ArrayList<>();
        var b = new ArsivSablonu.BolumBilgisi(" ", "", "github.com/x", 9, List.of("1-1", "a/b"), bos, "");
        List<String> h = ArsivSablonu.bilgiHatalari(b);
        assertEquals(6, h.size(), h.toString());
        assertTrue(ArsivSablonu.bilgiHatalari(elektrik(new MufredatAyari())).isEmpty());
        assertEquals("Seçmeli dersler `A/`, `B/` ve `C/` altında.", ArsivSablonu.havuzCumlesi(List.of("A", "B", "C")));
        assertEquals("", ArsivSablonu.havuzCumlesi(List.of()));
    }

    /** GitHub betikleri yeni arşivde gerçekten çalışır (python3 yoksa atlanır). */
    @Test
    void githubBetikleriYeniArsivdeCalisir(@TempDir Path gecici) throws Exception {
        assumeTrue(calistir(gecici, "python3", "--version") == 0, "python3 yok");
        Path kok = gecici.resolve("tek_elektrik");
        Path json = ArsivSablonu.yeniArsivOlustur(kok, elektrik(new MufredatAyari()), s -> { });
        assertEquals(0, calistir(kok, "git", "init", "-q"));
        assertEquals(0, calistir(kok, "git", "add", "-A"));

        assertEquals(0, calistir(kok, "python3", ".github/scripts/readme_gorselleri.py"));
        String readme = Files.readString(kok.resolve("README.md"));
        assertTrue(readme.contains("<!-- DOLULUK_BASLANGIC -->"), "Doluluk tablosu eklenmedi");
        // Formlar kurulmadan form betiği README'lere dokunmadan çıkar.
        String once = readme;
        assertEquals(0, calistir(kok, "python3", ".github/scripts/form_linkleri.py"));
        assertEquals(once, Files.readString(kok.resolve("README.md")));
        // Araç betiğin eklediği doluluk tablosunu korur.
        assertEquals(List.of(), new ReadmeGenerator(json, null).farkliOlanlar());
    }

    /** Bilgisayar arşivinin GitHub betikleri, iş akışları ve form betikleri aracın şablonuyla aynı olmalı. */
    @Test
    void mevcutArsivinAltyapisiSablonlaAyni() {
        String yol = System.getProperty("arsiv.json", "");
        Path json = yol.isEmpty() ? null : ReadmeGenerator.jsonKlasoruBul(Path.of(yol));
        assumeTrue(json != null, "arsiv.json verilmedi, test atlandı");
        assertEquals(List.of(), ArsivSablonu.altyapiyiGuncelle(ReadmeGenerator.repoKokuBul(json), false),
                "Arşivdeki betikler şablondan farklı: değişikliği src/main/resources/sablon'a da yapın "
                        + "(ya da arşivde Altyapıyı Güncelle)");
    }

    private static int calistir(Path dizin, String... komut) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(komut).directory(dizin.toFile()).redirectErrorStream(true).start();
        p.getInputStream().transferTo(System.out);
        return p.waitFor(60, TimeUnit.SECONDS) ? p.exitValue() : -1;
    }
}
