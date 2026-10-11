package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.MufredatAyari;
import edu.marmara.readme.engine.util.GithubSlug;
import edu.marmara.readme.engine.util.UrlKodla;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Birden fazla README'de kullanılan satır kalıpları (akademisyenler, form linkleri, varsayılan bloklar). */
final class Parcalar {

    static final String AKADEMISYENLER = "👨‍🏫 👩‍🏫 Dersi Yürüten Akademisyenler:";
    static final String HOCA_BOLUMU = "## 💬 Bu Dersi Veren Hocalar Hakkında";
    /** Ders sayfasındaki hoca yorumları bölümünde gösterilen son akademik yıl sayısı. */
    static final int SON_YIL = 2;
    /** Ders sayfasındaki hoca yorumları bölümünde hoca başına gösterilen yorum sayısı. */
    static final int OZETTE_YORUM = 2;
    private Parcalar() {
    }

    /**
     * "**2026-2027**" — eski müfredat dersi, konfigurasyon.json'daki geçiş yılı ve sonrasında açıldıysa
     * açıklamasıyla (ör. 2026 öncesi müfredat dersi 2026-2027'de).
     */
    static String yilEtiketi(Ders d, String yil, MufredatAyari m) {
        boolean eski = !m.eski_mufredat_oneki.isEmpty() && d.mufredat.startsWith(m.eski_mufredat_oneki)
                && yil.compareTo(m.eski_mufredat_notu_yili) >= 0;
        return "**" + yil + "**" + (eski ? " (" + m.eski_mufredat_notu + ")" : "");
    }

    /** Ders ve dönem README'sindeki yıl yıl hoca listesi (başlık hariç), her satır "\n" ile biter. */
    static String akademisyenListesi(Ders d, Map<String, Hoca> hocalar, MufredatAyari m) {
        StringBuilder sb = new StringBuilder();
        for (AkademisyenYili y : d.akademisyenler) {
            sb.append("- ").append(yilEtiketi(d, y.yil, m)).append(":\n");
            for (DersHocasi h : y.hocalar) {
                Hoca hoca = hocalar.get(h.ad);
                sb.append("  - ");
                if (hoca != null && !hoca.kisaltma.isEmpty()) {
                    sb.append("**").append(hoca.kisaltma).append("** — ");
                }
                sb.append(h.ad).append(notEki(h)).append("\n");
            }
        }
        return sb.toString();
    }

    /** Ana README ders kartındaki tek satır: en güncel yılın hocaları kısaltma linkleriyle. */
    static String kokAkademisyenSatiri(Ders d, Map<String, Hoca> hocalar, MufredatAyari m) {
        AkademisyenYili y = d.akademisyenler.get(0);
        List<String> adlar = new ArrayList<>();
        for (DersHocasi h : y.hocalar) {
            Hoca hoca = hocalar.get(h.ad);
            String ad = hoca != null && !hoca.kisaltma.isEmpty()
                    ? "[" + hoca.kisaltma + "](" + hocaCapasi(hoca) + ")"
                    : h.ad;
            adlar.add(ad + notEki(h));
        }
        return "    - " + yilEtiketi(d, y.yil, m) + ": " + String.join(", ", adlar) + "\n";
    }

    private static String notEki(DersHocasi h) {
        return h.not == null || h.not.isEmpty() ? "" : " (" + h.not + ")";
    }

    /** Ana README'deki hoca kartının bağlantısı, ör. "#‍-doç-dr-buket-doğan". */
    static String hocaCapasi(Hoca h) {
        return GithubSlug.slug(h.emoji() + " " + h.ad);
    }

    static String dersCapasi(String dersAdi) {
        return GithubSlug.slug("📘 " + dersAdi);
    }

    // ------------------------------------------------------------ form linkleri ve varsayılan bloklar

    private static String onDoldurulmus(Konfigurasyon k, String form, String alan, String deger) {
        return k.formAlani(form, "link") + "?usp=pp_url&entry." + k.formAlani(form, alan) + "=" + UrlKodla.kodla(deger);
    }

    static String hocaYorumLinki(Konfigurasyon k, String hoca) {
        return onDoldurulmus(k, "hoca_yorumlama", "hoca_alani", hoca);
    }

    /**
     * " Siz de [linkten](…) …" cümlesi; form henüz kurulmamışsa (yeni açılan bölüm arşivi) boş metin.
     * Form kurulunca GitHub betiği bu blokları linkli hâliyle yeniden yazar.
     */
    private static String linkCumlesi(Konfigurasyon k, String form, String alan, String deger, String sonu) {
        return k.formHazir(form, alan) ? " Siz de [linkten](" + onDoldurulmus(k, form, alan, deger) + ") " + sonu : "";
    }

    /** Henüz oy yokken GitHub betiğinin yazdığı yıldız bloğu. */
    static String varsayilanYildiz(String girinti, Konfigurasyon k, String ders) {
        return girinti + "- ⭐ **Yıldız Sayıları:**\n" + girinti + "    - ℹ️ Henüz yıldız veren yok."
                + linkCumlesi(k, "ders_oylama", "ders_alani", ders, "anonim şekilde oylamaya katılabilirsiniz.") + "\n";
    }

    /** Henüz onaylı yorum yokken GitHub betiğinin yazdığı ders yorum bloğu. */
    static String varsayilanGorus(String girinti, Konfigurasyon k, String ders) {
        return girinti + "- 💬 **Öğrenci Görüşleri:**\n" + girinti + "    - ℹ️ Henüz yorum yok."
                + linkCumlesi(k, "ders_yorumlama", "ders_alani", ders, "anonim şekilde görüşlerinizi belirtebilirsiniz.")
                + "\n";
    }

    static String varsayilanHocaGorus(Konfigurasyon k, String hoca) {
        return "- 💬 **Öğrenci Görüşleri:**\n  - ℹ️ Henüz yorum yok."
                + linkCumlesi(k, "hoca_yorumlama", "hoca_alani", hoca, "anonim şekilde görüşlerinizi belirtebilirsiniz.")
                + "\n";
    }

    // ------------------------------------------------------------ ders sayfasındaki hoca yorumları bölümü

    /**
     * "## 💬 Bu Dersi Veren Hocalar Hakkında" bölümü (sonundaki boş satırla); dersin akademisyen bilgisi
     * yoksa boş metin. Dersi son iki akademik yılda verenler, ana README hoca kartındaki onaylı yorumlarla.
     * GitHub'daki form_linkleri.py betiğinin hoca_bolumu fonksiyonunun aynısı.
     */
    static String hocaBolumu(Ders d, Map<String, Hoca> hocalar, KorunanBloklar korunan, Konfigurasyon k) {
        if (d.akademisyenler.isEmpty()) {
            return "";
        }
        List<AkademisyenYili> yillar = d.akademisyenler.subList(0, Math.min(SON_YIL, d.akademisyenler.size()));
        String aralik = yillar.size() == 1 ? yillar.get(0).yil
                : yillar.get(yillar.size() - 1).yil + " ve " + yillar.get(0).yil;
        StringBuilder sb = new StringBuilder(HOCA_BOLUMU).append("\n\n")
                .append("Dersi ").append(aralik).append(" akademik yıllarında veren hocalar. ")
                .append("Yorumlar hocanın tüm derslerini kapsar.\n\n");
        Set<String> gorulen = new LinkedHashSet<>();
        for (AkademisyenYili y : yillar) {
            for (DersHocasi h : y.hocalar) {
                if (!gorulen.add(h.ad)) {
                    continue;
                }
                Hoca hoca = hocalar.get(h.ad);
                if (hoca == null) {
                    sb.append("- **").append(h.ad).append("**\n");
                    continue;
                }
                String kart = "../../README.md" + hocaCapasi(hoca);
                sb.append("- **[").append(h.ad).append("](").append(kart).append(")**\n");
                List<String> yorumlar = korunan.hocaYorumlari(h.ad);
                for (String yorum : yorumlar.subList(0, Math.min(OZETTE_YORUM, yorumlar.size()))) {
                    sb.append("    - ").append(yorum).append("\n");
                }
                if (yorumlar.size() > OZETTE_YORUM) {
                    sb.append("    - ℹ️ Diğer ").append(yorumlar.size() - OZETTE_YORUM)
                            .append(" yoruma [hocanın kartından](").append(kart).append(") erişebilirsiniz.\n");
                }
                if (k.formHazir("hoca_yorumlama", "hoca_alani")) {
                    sb.append("    - ✍️ ").append(yorumlar.isEmpty() ? "Henüz yorum yok. " : "")
                            .append("Siz de [linkten](").append(hocaYorumLinki(k, h.ad))
                            .append(") bu hoca hakkında anonim şekilde görüşlerinizi belirtebilirsiniz.\n");
                } else if (yorumlar.isEmpty()) {
                    sb.append("    - ✍️ Henüz yorum yok.\n");
                }
            }
        }
        return sb.append("\n").toString();
    }
}
