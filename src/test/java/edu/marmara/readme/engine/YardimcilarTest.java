package edu.marmara.readme.engine;

import edu.marmara.readme.engine.util.GithubSlug;
import edu.marmara.readme.engine.util.TurkceSira;
import edu.marmara.readme.engine.util.UrlKodla;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** README'deki linklerin ve sıralamaların GitHub/Python ile aynı çıktığını doğrular. */
class YardimcilarTest {

    @Test
    void githubBaslikBaglantisi() {
        // Emoji silinir ama içindeki birleştirici U+200D kalır; Türkçe büyük İ, Unicode küçültmede "i̇" olur.
        assertEquals("#‍-doç-dr-buket-doğan", GithubSlug.slug("👨‍🏫 Doç. Dr. Buket Doğan"));
        assertEquals("#-i̇ngilizce-1", GithubSlug.slug("📘 İngilizce 1"));
        assertEquals("#-yapay-zekâya-giriş", GithubSlug.slug("📘 Yapay Zekâya Giriş"));
    }

    @Test
    void pythonQuoteIleAyniKodlama() {
        assertEquals("Mesleki%20Se%C3%A7meli/Web%20Programlama", UrlKodla.kodla("Mesleki Seçmeli/Web Programlama"));
        assertEquals("Do%C3%A7.%20Dr.%20Alper%20Nabi%20Akpolat", UrlKodla.kodla("Doç. Dr. Alper Nabi Akpolat"));
        assertEquals("C%2B%2B%20%28lab%29", UrlKodla.kodla("C++ (lab)"));
    }

    @Test
    void turkceAlfabeSirasi() {
        List<String> l = new ArrayList<>(List.of("AS", "AÖ", "AND", "ABal", "ABA", "Bİ", "BÇU", "BD"));
        l.sort(TurkceSira.KARSILASTIRICI);
        assertEquals(List.of("ABA", "ABal", "AND", "AÖ", "AS", "BÇU", "BD", "Bİ"), l);
        assertEquals("Ayşe Berna Altınel", TurkceSira.unvansiz("Doç. Dr. Ayşe Berna Altınel"));
        assertEquals("Mehmet Vehbi Büker", TurkceSira.unvansiz("Öğr. Gör. Dr. Mehmet Vehbi Büker"));
    }
}
