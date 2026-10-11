package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.Havuz;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.OneriGrubu;
import edu.marmara.readme.engine.util.UrlKodla;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dönem klasörlerinin (1-1 … 4-2) ve seçmeli havuz klasörlerinin README.md'leri. */
public class DonemWriter {

    private final Map<String, Hoca> hocalar;
    private final KorunanBloklar korunan;
    private final Konfigurasyon konfig;

    public DonemWriter(Map<String, Hoca> hocalar, KorunanBloklar korunan, Konfigurasyon konfig) {
        this.hocalar = hocalar;
        this.korunan = korunan;
        this.konfig = konfig;
    }

    /** Dönem README'si: genel tavsiyeler ve dönemin derslerinin ayrıntıları. */
    public String yaz(Donem donem, List<Ders> dersler) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 📅 ").append(donem.donem_adi).append("\n\n");
        if (!donem.genel_tavsiyeler.isEmpty()) {
            sb.append("## 📝 Genel Tavsiyeler\n\n");
            for (String t : donem.genel_tavsiyeler) {
                sb.append("- 💡 ").append(t).append("\n");
            }
        }
        sb.append("## 📚 Dönemin Zorunlu Dersleri\n\n\n");
        for (Ders d : dersler) {
            sb.append("### 📘 ").append(d.ad).append("\n\n");
            sb.append("#### 📄 Ders Bilgileri\n\n");
            sb.append("- 📅 **Yıl:** ").append(d.yil).append("\n");
            sb.append("- 📆 **Dönem:** ").append(d.donem).append("\n");
            sb.append("- 🏫 **Ders Tipi:** ").append(d.tip).append("\n");
            sb.append("- 🏷️ **Müfredat:** ").append(d.mufredat).append("\n");
            if (!d.mufredat_notu.isEmpty()) {
                sb.append("    - ℹ️ ").append(d.mufredat_notu).append("\n");
            }
            String yildiz = korunan.donemYildiz(donem.klasor, d.ad);
            String gorus = korunan.donemGorus(donem.klasor, d.ad);
            sb.append(yildiz != null ? yildiz : Parcalar.varsayilanYildiz("", konfig, d.ad));
            sb.append(gorus != null ? gorus : Parcalar.varsayilanGorus("", konfig, d.ad));
            sb.append("\n");
            if (!d.derse_dair_oneriler.isEmpty()) {
                sb.append("#### 💡 Derse Dair Öneriler\n\n");
                for (OneriGrubu g : d.derse_dair_oneriler) {
                    sb.append("##### 📌 Öneri sahibi: ").append(g.oneri_sahibi).append("\n");
                    g.oneriler.forEach(o -> sb.append("- ").append(o).append("\n"));
                }
                sb.append("\n");
            }
            if (!d.akademisyenler.isEmpty()) {
                sb.append("#### ").append(Parcalar.AKADEMISYENLER).append("\n");
                sb.append(Parcalar.akademisyenListesi(d, hocalar)).append("\n");
            }
        }
        return sb.toString();
    }

    /** Seçmeli havuz README'si: açıklama ve derslerin listesi (karışık müfredatlıysa etiketleriyle). */
    public String yazHavuz(Havuz havuz, List<Ders> dersler) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 📚 ").append(havuz.ad).append("\n\n");
        sb.append(havuz.aciklama).append("\n\n");
        sb.append("## 📋 Ders Listesi\n\n\n");
        Set<String> mufredatlar = new HashSet<>();
        dersler.forEach(d -> mufredatlar.add(d.mufredat));
        boolean etiketli = mufredatlar.size() > 1;
        for (Ders d : dersler) {
            sb.append("- 📘 [").append(d.ad).append("](./").append(UrlKodla.kodla(d.klasorAdi())).append("/)");
            if (etiketli) {
                sb.append(" _(🏷️ ").append(d.mufredat).append(")_");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
