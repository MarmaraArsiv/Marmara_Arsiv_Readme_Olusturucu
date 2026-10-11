package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.OneriGrubu;

import java.util.Map;

/** Bir dersin kendi klasöründeki README.md. */
public class DersKlasorWriter {

    private final Map<String, Hoca> hocalar;
    private final KorunanBloklar korunan;
    private final Konfigurasyon konfig;
    private final String katkidaBulunMetni;

    public DersKlasorWriter(Map<String, Hoca> hocalar, KorunanBloklar korunan, Konfigurasyon konfig,
                            String katkidaBulunMetni) {
        this.hocalar = hocalar;
        this.korunan = korunan;
        this.konfig = konfig;
        this.katkidaBulunMetni = katkidaBulunMetni;
    }

    public String yaz(Ders d) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 📚 ").append(d.ad).append("\n\n");
        sb.append("## ℹ️ Ders Bilgileri\n\n");
        if (d.yil > 0) {
            sb.append("- 📅 **Yıl:** ").append(d.yil).append("\n");
            sb.append("- 📆 **Dönem:** ").append(d.donem).append("\n");
            sb.append("- 🏫 **Ders Tipi:** ").append(d.tip).append("\n");
        } else {
            sb.append("- 🏷️ **Ders Tipi:** ").append(d.tip).append("\n");
        }
        sb.append("- 🏷️ **Müfredat:** ").append(d.mufredat).append("\n");
        if (!d.mufredat_notu.isEmpty()) {
            sb.append("    - ℹ️ ").append(d.mufredat_notu).append("\n");
        }
        String yildiz = korunan.dersYildiz(d.klasor);
        String gorus = korunan.dersGorus(d.klasor);
        sb.append(yildiz != null ? yildiz : Parcalar.varsayilanYildiz("", konfig, d.klasorAdi()));
        sb.append(gorus != null ? gorus : Parcalar.varsayilanGorus("", konfig, d.klasorAdi()));
        sb.append("\n");

        if (!d.derse_dair_oneriler.isEmpty()) {
            sb.append("## 📝 Derse Dair Öneriler\n\n");
            for (OneriGrubu g : d.derse_dair_oneriler) {
                sb.append("### 💡 Öneri sahibi: ").append(g.oneri_sahibi).append("\n");
                for (String oneri : g.oneriler) {
                    sb.append("- ").append(oneri).append("\n");
                }
            }
            sb.append("\n");
        }
        if (!d.akademisyenler.isEmpty()) {
            sb.append("## ").append(Parcalar.AKADEMISYENLER).append("\n");
            sb.append(Parcalar.akademisyenListesi(d, hocalar)).append("\n");
        }
        sb.append(Parcalar.hocaBolumu(d, hocalar, korunan, konfig));
        sb.append("## 🤝 Katkıda Bulun\n\n").append(katkidaBulunMetni).append("\n");
        return sb.toString();
    }
}
