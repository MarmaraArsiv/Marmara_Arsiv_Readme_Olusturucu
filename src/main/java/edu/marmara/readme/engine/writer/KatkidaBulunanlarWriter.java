package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.IletisimBilgisi;
import edu.marmara.readme.engine.model.KatkidaBulunan;
import edu.marmara.readme.engine.model.KatkidaBulunanlarBolumu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ana README'nin "🤝 Katkıda Bulunanlar" bölümü. GitHub'daki katkida_bulunanlari_guncelle.py
 * betiğinin readme_bolumu fonksiyonunun aynısı: katkı seviyesine göre (Çok → h1) başlık boyutu,
 * profil fotoğrafı ve iletişim linkleri başlığın içinde.
 */
public class KatkidaBulunanlarWriter {

    private static final List<Integer> AVATAR_BOYUTLARI = List.of(100, 80, 64, 56, 48, 40);
    private static final Pattern GITHUB = Pattern.compile("^https?://github\\.com/([^/?#]+)");

    /** Bölümün tamamı, "</details>\n" ile biter. */
    public String yaz(KatkidaBulunanlarBolumu veri) {
        String bolumAdi = veri.bolum_adi == null || veri.bolum_adi.isEmpty()
                ? Sabitler.VARSAYILAN_KATKIDA_BULUNANLAR_BOLUM_ADI : veri.bolum_adi;
        List<String> satirlar = new ArrayList<>();
        satirlar.add("<details>");
        satirlar.add("<summary><b>🤝 " + bolumAdi + "</b></summary>\n");
        satirlar.add("<h2 align='center'>🤝 " + bolumAdi + "</h2>\n");
        if (veri.bolum_aciklamasi != null && !veri.bolum_aciklamasi.isEmpty()) {
            satirlar.add(veri.bolum_aciklamasi + "\n");
        }
        List<KatkidaBulunan> sirali = new ArrayList<>(veri.katkida_bulunanlar);
        sirali.sort(Comparator.comparingInt((KatkidaBulunan k) -> oranIndex(k.katkida_bulunma_orani))
                .thenComparing(k -> k.ad == null ? "" : k.ad));
        for (KatkidaBulunan k : sirali) {
            int idx = oranIndex(k.katkida_bulunma_orani);
            String emoji = Sabitler.KATKIDA_EMOJILER.get(Math.min(idx, Sabitler.KATKIDA_EMOJILER.size() - 1));
            String etiket = "h" + Math.min(idx + 1, 6);
            String login = githubKullanici(k.github_link);
            if (login != null) {
                int boyut = AVATAR_BOYUTLARI.get(Math.min(idx, AVATAR_BOYUTLARI.size() - 1));
                satirlar.add("<p align='center'><a href='" + k.github_link + "'>"
                        + "<img src='https://github.com/" + login + ".png?size=" + (boyut * 2) + "' "
                        + "width='" + boyut + "' alt='" + k.ad + "'></a></p>");
            }
            // Linkler başlığın içinde: GitHub h1/h2 altına çizgi çeker, çizgi linklerin altında kalsın.
            StringBuilder linkler = new StringBuilder();
            if (!k.iletisim_bilgileri.isEmpty()) {
                List<String> parcalar = new ArrayList<>();
                for (IletisimBilgisi b : k.iletisim_bilgileri) {
                    parcalar.add("<a href='" + b.link + "'>" + b.baslik + "</a>");
                }
                linkler.append("<br><sub>").append(String.join(" &nbsp;·&nbsp; ", parcalar)).append("</sub>");
            }
            satirlar.add("<" + etiket + " align='center'>" + emoji + " <b><i>" + k.ad + "</i></b> " + emoji
                    + linkler + "</" + etiket + ">");
            satirlar.add("");
        }
        satirlar.add("</details>\n");
        return String.join("\n", satirlar);
    }

    private static int oranIndex(String oran) {
        int i = Sabitler.KATKIDA_BULUNMA_ORANI_DIZI.indexOf(oran);
        return i >= 0 ? i : Sabitler.KATKIDA_BULUNMA_ORANI_DIZI.size() - 1;
    }

    private static String githubKullanici(String link) {
        Matcher m = GITHUB.matcher(link == null ? "" : link);
        return m.find() ? m.group(1).toLowerCase(Locale.ROOT) : null;
    }
}
