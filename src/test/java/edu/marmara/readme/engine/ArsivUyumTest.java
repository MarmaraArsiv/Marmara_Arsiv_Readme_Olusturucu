package edu.marmara.readme.engine;

import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.KatkidaBulunanlarBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.RepoKullanimiBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Gerçek arşive karşı çalışan testler. Arşivin JSON klasörü verilmezse atlanır:
 * <pre>./mvnw test -Darsiv.json=../Marmara_Bilgisayar_Muhendisligi_Arsiv</pre>
 */
class ArsivUyumTest {

    /** -Darsiv.json: arşiv reposunun kökü ya da JSON klasörü. */
    private static Path jsonKlasoru() {
        String yol = System.getProperty("arsiv.json", "");
        Path klasor = yol.isEmpty() ? null : ReadmeGenerator.jsonKlasoruBul(Path.of(yol));
        assumeTrue(klasor != null, "arsiv.json verilmedi ya da JSON klasörü bulunamadı, test atlandı");
        return klasor;
    }

    /** JSON'dan üretilen README'ler arşivdekilerle birebir aynı olmalı (hiçbir dosya yazılmaz). */
    @Test
    void readmelerJsonIleUyumlu() {
        List<Path> farkli = new ReadmeGenerator(jsonKlasoru(), null).farkliOlanlar();
        assertEquals(List.of(), farkli, "JSON'dan üretilenden farklı README'ler");
    }

    /** Araç bir JSON'u okuyup geri yazınca dosya bayt bayt aynı kalmalı (arayüzden kaydetmek fark üretmesin). */
    @Test
    void jsonOkuYazAyniKalir() throws IOException {
        Path dizin = jsonKlasoru();
        Map<String, Class<?>> dosyalar = Map.of(
                ReadmeGenerator.DERSLER_JSON, DerslerBolumu.class,
                ReadmeGenerator.HOCALAR_JSON, HocalarBolumu.class,
                ReadmeGenerator.DONEMLER_JSON, DonemlerBolumu.class,
                ReadmeGenerator.GIRIS_JSON, GirisBolumu.class,
                ReadmeGenerator.REPO_KULLANIMI_JSON, RepoKullanimiBolumu.class,
                ReadmeGenerator.KATKIDA_BULUNANLAR_JSON, KatkidaBulunanlarBolumu.class,
                ReadmeGenerator.KONFIGURASYON_JSON, Konfigurasyon.class);
        for (Map.Entry<String, Class<?>> e : dosyalar.entrySet()) {
            String asil = Files.readString(dizin.resolve(e.getKey()), StandardCharsets.UTF_8);
            Object veri = JsonUtil.MAPPER.readValue(asil, e.getValue());
            assertEquals(asil, JsonUtil.metin(veri), e.getKey() + " okunup yazılınca değişti");
        }
    }
}
