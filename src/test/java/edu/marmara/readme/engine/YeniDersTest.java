package edu.marmara.readme.engine;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Arşivde henüz olmayan bir ders eklenince README'lerin doğru üretildiğini denetler (hiçbir dosya yazılmaz):
 * ders README'si GitHub bloklarının boş hâliyle, havuz listesi, ana sayfa kartı ve hoca kartı.
 */
class YeniDersTest {

    @Test
    void yeniSecmeliDersVeHocasi(@TempDir Path gecici) throws IOException {
        String yol = System.getProperty("arsiv.json", "");
        Path kaynak = yol.isEmpty() ? null : ReadmeGenerator.jsonKlasoruBul(Path.of(yol));
        assumeTrue(kaynak != null, "arsiv.json verilmedi, test atlandı");
        try (Stream<Path> dosyalar = Files.list(kaynak)) {
            for (Path f : dosyalar.toList()) {
                Files.copy(f, gecici.resolve(f.getFileName()));
            }
        }
        // README'ler gerçek arşivden okunur (korunan bloklar için), JSON geçici kopyadan.
        Konfigurasyon konfig = JsonUtil.read(gecici.resolve(ReadmeGenerator.KONFIGURASYON_JSON), Konfigurasyon.class);
        konfig.dokumanlar_repo_yolu = kaynak.resolve(konfig.dokumanlar_repo_yolu).normalize().toString();
        JsonUtil.write(gecici.resolve(ReadmeGenerator.KONFIGURASYON_JSON), konfig);

        DerslerBolumu dersler = JsonUtil.read(gecici.resolve(ReadmeGenerator.DERSLER_JSON), DerslerBolumu.class);
        Ders d = new Ders();
        d.ad = "Test Dersi";
        d.klasor = "Mesleki Seçmeli/Test Dersi";
        d.tip = "Seçmeli";
        d.mufredat = "2026 Sonrası";
        d.mufredat_notu = "Deneme notu.";
        AkademisyenYili y = new AkademisyenYili();
        y.yil = "2026-2027";
        y.hocalar.add(new DersHocasi("Doç. Dr. Buket Doğan", "lab"));
        y.hocalar.add(new DersHocasi("Listede Olmayan Hoca", ""));
        d.akademisyenler.add(y);
        dersler.dersler.add(d);
        JsonUtil.write(gecici.resolve(ReadmeGenerator.DERSLER_JSON), dersler);

        Map<Path, String> cikti = new ReadmeGenerator(gecici, null).uret();

        String dersReadme = cikti.get(Path.of("Mesleki Seçmeli/Test Dersi/README.md"));
        assertEquals("""
                # 📚 Test Dersi

                ## ℹ️ Ders Bilgileri

                - 🏷️ **Ders Tipi:** Seçmeli
                - 🏷️ **Müfredat:** 2026 Sonrası
                    - ℹ️ Deneme notu.
                - ⭐ **Yıldız Sayıları:**
                    - ℹ️ Henüz yıldız veren yok. Siz de [linkten](%s?usp=pp_url&entry.%s=Test%%20Dersi) anonim şekilde oylamaya katılabilirsiniz.
                - 💬 **Öğrenci Görüşleri:**
                    - ℹ️ Henüz yorum yok. Siz de [linkten](%s?usp=pp_url&entry.%s=Test%%20Dersi) anonim şekilde görüşlerinizi belirtebilirsiniz.

                ## 👨‍🏫 👩‍🏫 Dersi Yürüten Akademisyenler:
                - **2026-2027**:
                  - **BD** — Doç. Dr. Buket Doğan (lab)
                  - Listede Olmayan Hoca

                """.formatted(konfig.formAlani("ders_oylama", "link"), konfig.formAlani("ders_oylama", "ders_alani"),
                konfig.formAlani("ders_yorumlama", "link"), konfig.formAlani("ders_yorumlama", "ders_alani")),
                dersReadme.substring(0, dersReadme.indexOf("## 💬 Bu Dersi Veren Hocalar Hakkında")));
        assertTrue(dersReadme.contains("Dersi 2026-2027 akademik yıllarında veren hocalar."));
        assertTrue(dersReadme.contains("- **[Doç. Dr. Buket Doğan](../../README.md#‍-doç-dr-buket-doğan)**\n"));
        assertTrue(dersReadme.contains("- **Listede Olmayan Hoca**\n"));
        assertTrue(dersReadme.endsWith("## 🤝 Katkıda Bulun\n\n" + dersler.katkida_bulun_metni + "\n"));

        String havuz = cikti.get(Path.of("Mesleki Seçmeli/README.md"));
        assertTrue(havuz.endsWith("- 📘 [Test Dersi](./Test%20Dersi/) _(🏷️ 2026 Sonrası)_\n"));

        String kok = cikti.get(Path.of("README.md"));
        assertTrue(kok.contains("#### 📘 Test Dersi \n  - 🏷️ **Ders Tipi:** Seçmeli\n  - 🏷️ **Müfredat:** 2026 Sonrası\n"
                + "      - ℹ️ Deneme notu.\n  - ⭐ **Yıldız Sayıları:**\n"));
        assertTrue(kok.contains("    - **2026-2027**: [BD](#‍-doç-dr-buket-doğan) (lab), Listede Olmayan Hoca\n"
                + "  - 📂 [Ders Klasörü](./Mesleki%20Se%C3%A7meli/Test%20Dersi)\n"));
        assertTrue(kok.contains("  - 📖 [Test Dersi](#-test-dersi) (2026-2027)\n"), "hoca kartında yeni ders");
    }
}
