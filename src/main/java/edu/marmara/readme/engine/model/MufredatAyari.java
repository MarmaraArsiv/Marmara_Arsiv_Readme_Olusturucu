package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * konfigurasyon.json "mufredat" alanı: bölümün müfredat seçenekleri ve eski müfredat notu kuralı.
 * Her bölümün müfredat geçişi farklı olabilir; varsayılanlar Bilgisayar Mühendisliği'nin 2026 geçişidir.
 */
public class MufredatAyari extends JsonNesnesi {

    /** Ders düzenleme penceresindeki müfredat seçenekleri; ilki yeni derslerin varsayılanıdır. */
    public List<String> secenekler = new ArrayList<>(
            List.of("Her İki Müfredat", "2026 Öncesi", "2026 Sonrası", "2026 Öncesi (Zorunlu)"));

    /**
     * Müfredatı bu önekle başlayan bir ders, {@link #eski_mufredat_notu_yili} ve sonrasında açıldıysa
     * yıl etiketinin yanına {@link #eski_mufredat_notu} yazılır. Boşsa kural kapalıdır.
     */
    public String eski_mufredat_oneki = "2026 Öncesi";
    public String eski_mufredat_notu_yili = "2026-2027";
    public String eski_mufredat_notu =
            "bu yıl yeni girenler bu dersi alamaz; ders yalnızca 2026 öncesi müfredattaki öğrenciler için açıldı";
}
