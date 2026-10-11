package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.model.Havuz;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocaGrubu;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.KatkidaBulunanlarBolumu;
import edu.marmara.readme.engine.model.Kavram;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.RepoKullanimiBolumu;
import edu.marmara.readme.engine.util.TurkceSira;
import edu.marmara.readme.engine.util.UrlKodla;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repo kökündeki ana README.md. */
public class AnaReadmeWriter {

    private final GirisBolumu giris;
    private final RepoKullanimiBolumu repoKullanimi;
    private final DerslerBolumu dersler;
    private final HocalarBolumu hocalar;
    private final DonemlerBolumu donemler;
    private final KatkidaBulunanlarBolumu katkidaBulunanlar;
    private final Konfigurasyon konfig;
    private final KorunanBloklar korunan;
    private final Map<String, Hoca> hocaAdina;

    public AnaReadmeWriter(GirisBolumu giris, RepoKullanimiBolumu repoKullanimi, DerslerBolumu dersler,
                           HocalarBolumu hocalar, DonemlerBolumu donemler,
                           KatkidaBulunanlarBolumu katkidaBulunanlar, Konfigurasyon konfig,
                           KorunanBloklar korunan, Map<String, Hoca> hocaAdina) {
        this.giris = giris;
        this.repoKullanimi = repoKullanimi;
        this.dersler = dersler;
        this.hocalar = hocalar;
        this.donemler = donemler;
        this.katkidaBulunanlar = katkidaBulunanlar;
        this.konfig = konfig;
        this.korunan = korunan;
        this.hocaAdina = hocaAdina;
    }

    public String yaz() {
        StringBuilder sb = new StringBuilder();
        yazGiris(sb);
        yazRepoKullanimi(sb);
        if (korunan.doluluk() != null) {
            sb.append(korunan.doluluk()).append("\n");
        }
        yazDersler(sb);
        yazSecmeliler(sb);
        yazHocalar(sb);
        yazKisaltmalar(sb);
        sb.append(new KatkidaBulunanlarWriter().yaz(katkidaBulunanlar));
        String repo = konfig.github_url.replaceFirst("^https?://github\\.com/", "").replaceAll("/+$", "");
        sb.append("\n\n## Yıldız Geçmişi\n[![Star History Chart](https://api.star-history.com/svg?repos=")
                .append(repo).append("&type=Date)](https://star-history.com/#").append(repo).append("&Date)\n");
        return sb.toString();
    }

    private void yazGiris(StringBuilder sb) {
        sb.append("# 📖 ").append(giris.baslik).append("\n\n").append(giris.aciklama).append("\n\n");
        sb.append(" ## 🗣️ Geri Bildirimde Bulunun\n\n").append(giris.geri_bildirim_aciklamasi).append("\n\n");
        String hocaYorum = konfig.formAlani("hoca_yorumlama", "link");
        String dersYorum = konfig.formAlani("ders_yorumlama", "link");
        String dersOylama = konfig.formAlani("ders_oylama", "link");
        if (hocaYorum.isBlank() && dersYorum.isBlank() && dersOylama.isBlank()) {
            sb.append("- ℹ️ Geri bildirim formları yakında eklenecek.\n\n");
        } else {
            sb.append("- [✍️ **Hocalar için yorum linki**](").append(hocaYorum).append(")\n");
            sb.append("- [✍️ **Dersler için yorum linki**](").append(dersYorum).append(")\n");
            sb.append("- [⭐ **Dersler için oylama linki**](").append(dersOylama).append(")\n\n");
        }
        // Arayüzden düzenlenen ham metinler: sondaki boşluklar ne olursa olsun bölüm bir boş satırla biter.
        if (!giris.katkida_bulunun.isBlank()) {
            sb.append(giris.katkida_bulunun.stripTrailing()).append("\n\n");
        }
        if (!giris.sayac.isBlank()) {
            sb.append(giris.sayac.stripTrailing()).append("\n\n");
        }
        sb.append("<details>\n<summary><b>🗂 İçindekiler</b></summary>\n\n## 🗂 İçindekiler\n\n");
        for (String bolum : giris.icindekiler) {
            sb.append("- 🔗 ").append(bolum).append("\n");
        }
        sb.append("</details>\n\n");
    }

    private void yazRepoKullanimi(StringBuilder sb) {
        RepoKullanimiBolumu r = repoKullanimi;
        sb.append("<details>\n<summary><b>🛠 ").append(r.baslik).append("</b></summary>\n\n\n\n\n");
        sb.append("## 🛠 ").append(r.baslik).append("\n\n");
        sb.append("### ⚙️ ").append(r.aciklama).append(":\n");
        r.aciklamalar.forEach(a -> sb.append("- 📋 ").append(a).append("\n"));
        sb.append("\n\n### 📝 ").append(r.talimat).append(":\n");
        r.talimatlar.forEach(t -> sb.append("- 👉 ").append(t).append("\n"));
        sb.append("</details>\n\n");
        if (r.kavramlar.isEmpty()) {
            return;
        }
        sb.append("<details>\n<summary><b>🔍 ").append(r.kavram).append("</b></summary>\n\n");
        sb.append("## 🔍 ").append(r.kavram).append("\n\n");
        if (!r.kavram_aciklamasi.isEmpty()) {
            sb.append(r.kavram_aciklamasi).append("\n\n");
        }
        for (Kavram k : r.kavramlar) {
            sb.append("- 💡 **").append(k.kavram).append("**\n");
            k.aciklamalar.forEach(a -> sb.append("  - 📘 ").append(a).append("\n"));
        }
        sb.append("</details>\n\n");
    }

    private void yazDersler(StringBuilder sb) {
        sb.append("<details>\n<summary><b>📖 ").append(dersler.bolum_adi).append("</b></summary>\n\n\n\n\n");
        sb.append("## 📖 ").append(dersler.bolum_adi).append("\n").append(dersler.bolum_aciklamasi).append("\n\n\n\n\n");
        List<Donem> liste = donemler.donemler;
        for (int i = 0; i < liste.size(); i++) {
            Donem donem = liste.get(i);
            sb.append("### 🗓 ").append(donem.yil).append(". Yıl - ").append(donem.donem).append("\n\n\n");
            sb.append(String.join("\n\n\n", kartlar(donem.klasor)));
            sb.append(i + 1 < liste.size() ? "\n\n" : "\n</details>\n\n");
        }
    }

    private void yazSecmeliler(StringBuilder sb) {
        sb.append("<details>\n<summary><b>🧩 ").append(dersler.secmeli_bolum_adi).append("</b></summary>\n\n");
        sb.append("## 🧩 ").append(dersler.secmeli_bolum_adi).append("\n")
                .append(dersler.secmeli_bolum_aciklamasi).append("\n\n");
        for (Havuz havuz : donemler.havuzlar) {
            sb.append("### 🗂 ").append(havuz.ad).append("\n\n");
            sb.append(String.join("\n\n", kartlar(havuz.klasor))).append("\n\n");
        }
        sb.append("</details>\n\n");
    }

    private List<String> kartlar(String ustKlasor) {
        List<String> sonuc = new ArrayList<>();
        for (Ders d : dersler.dersler) {
            if (d.ustKlasor().equals(ustKlasor)) {
                sonuc.add(dersKarti(d));
            }
        }
        return sonuc;
    }

    /** Ana README ders kartı; sonunda satır sonu yok. */
    private String dersKarti(Ders d) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### 📘 ").append(d.ad).append(" \n");
        sb.append("  - 🏷️ **Ders Tipi:** ").append(d.tip).append("\n");
        sb.append("  - 🏷️ **Müfredat:** ").append(d.mufredat).append("\n");
        if (!d.mufredat_notu.isEmpty()) {
            sb.append("      - ℹ️ ").append(d.mufredat_notu).append("\n");
        }
        String yildiz = korunan.kokYildiz(d.klasor);
        String gorus = korunan.kokGorus(d.klasor);
        sb.append(yildiz != null ? yildiz : Parcalar.varsayilanYildiz("  ", konfig, d.klasorAdi()));
        sb.append(gorus != null ? gorus : Parcalar.varsayilanGorus("  ", konfig, d.klasorAdi()));
        if (!d.akademisyenler.isEmpty()) {
            sb.append("  - ").append(Parcalar.AKADEMISYENLER.replace("Dersi Yürüten Akademisyenler:",
                    "**Dersi Yürüten Akademisyenler:**")).append("\n");
            sb.append(Parcalar.kokAkademisyenSatiri(d, hocaAdina, konfig.mufredat));
        }
        sb.append("  - 📂 [Ders Klasörü](./").append(UrlKodla.kodla(d.klasor)).append(")");
        return sb.toString();
    }

    private void yazHocalar(StringBuilder sb) {
        sb.append("<details>\n<summary><b>🎓 ").append(hocalar.bolum_adi).append("</b></summary>\n\n\n\n\n");
        sb.append("## 🎓 ").append(hocalar.bolum_adi).append("\n").append(hocalar.bolum_aciklamasi).append("\n\n\n\n\n");
        for (HocaGrubu grup : hocalar.gruplar) {
            List<Hoca> gruptakiler = new ArrayList<>();
            for (Hoca h : hocalar.hocalar) {
                if (grup.ad.equals(h.grup)) {
                    gruptakiler.add(h);
                }
            }
            if (gruptakiler.isEmpty()) {
                continue;
            }
            gruptakiler.sort(Comparator.comparing(h -> TurkceSira.unvansiz(h.ad), TurkceSira.KARSILASTIRICI));
            sb.append("### ").append(grup.ad).append("\n\n\n");
            if (!grup.aciklama.isEmpty()) {
                sb.append(grup.aciklama).append("\n\n");
            }
            for (Hoca h : gruptakiler) {
                sb.append(hocaKarti(h)).append("\n\n");
            }
        }
        sb.append("</details>\n\n");
    }

    private String hocaKarti(Hoca h) {
        StringBuilder sb = new StringBuilder();
        sb.append("#### ").append(h.emoji()).append(" ").append(h.ad).append(" \n");
        sb.append("- 🚪 **Ofis:** ").append(h.ofis).append("\n");
        sb.append("- 🔗 **Araştırma Sayfası:** ")
                .append(h.link.isEmpty() ? "" : "[" + h.link + "](" + h.link + ")").append("\n");
        String gorus = korunan.hocaGorus(h.ad);
        sb.append(gorus != null ? gorus : Parcalar.varsayilanHocaGorus(konfig, h.ad));
        sb.append("- 📚 **Verdiği Dersler:**\n");
        sb.append(String.join("\n", verdigiDersler(h)));
        return sb.toString();
    }

    /**
     * Hoca kartındaki ders satırları: aynı adlı derslerin yılları birleşir; en son yıla göre azalan,
     * eşitlikte hocalar.json "dersler" sırası.
     */
    private List<String> verdigiDersler(Hoca h) {
        Map<String, List<String>> yillar = new LinkedHashMap<>();
        for (Ders d : dersler.dersler) {
            for (AkademisyenYili y : d.akademisyenler) {
                for (DersHocasi dh : y.hocalar) {
                    if (dh.ad.equals(h.ad)) {
                        List<String> l = yillar.computeIfAbsent(d.ad, a -> new ArrayList<>());
                        if (!l.contains(y.yil)) {
                            l.add(y.yil);
                        }
                    }
                }
            }
        }
        List<String> sira = new ArrayList<>();
        for (String ad : h.dersler) {
            if (yillar.containsKey(ad) && !sira.contains(ad)) {
                sira.add(ad);
            }
        }
        for (String ad : yillar.keySet()) {
            if (!sira.contains(ad)) {
                sira.add(ad);
            }
        }
        sira.sort(Comparator.comparing((String ad) -> yillar.get(ad).stream().max(String::compareTo).orElse(""))
                .reversed());
        List<String> satirlar = new ArrayList<>();
        for (String ad : sira) {
            satirlar.add("  - 📖 [" + ad + "](" + Parcalar.dersCapasi(ad) + ") (" + yilAraligi(yillar.get(ad)) + ")");
        }
        if (satirlar.isEmpty()) {
            satirlar.add("  - 📖 Ders bilgileri bulunamadı.");
        }
        return satirlar;
    }

    /** ["2022-2023","2023-2024","2025-2026"] → "2022-2023 → 2023-2024, 2025-2026". */
    static String yilAraligi(List<String> yillar) {
        List<Integer> bas = yillar.stream().map(y -> Integer.parseInt(y.substring(0, 4))).sorted().toList();
        List<String> parcalar = new ArrayList<>();
        int i = 0;
        while (i < bas.size()) {
            int j = i;
            while (j + 1 < bas.size() && bas.get(j + 1) == bas.get(j) + 1) {
                j++;
            }
            String ilk = bas.get(i) + "-" + (bas.get(i) + 1);
            parcalar.add(i == j ? ilk : ilk + " → " + bas.get(j) + "-" + (bas.get(j) + 1));
            i = j + 1;
        }
        return String.join(", ", parcalar);
    }

    private void yazKisaltmalar(StringBuilder sb) {
        sb.append("<details>\n<summary><b>🆎 Hoca Kısaltmaları</b></summary>\n\n");
        sb.append("<h2 align='center'>🆎 Hoca Kısaltmaları</h2>\n\n");
        List<Hoca> liste = new ArrayList<>(hocalar.hocalar.stream().filter(h -> !h.kisaltma.isEmpty()).toList());
        liste.sort(Comparator.comparing(h -> h.kisaltma, TurkceSira.KARSILASTIRICI));
        for (Hoca h : liste) {
            sb.append("<p align='center'>🔹 <b>").append(h.kisaltma).append("</b> &emsp; ").append(h.ad).append(" 🔹</p>\n");
        }
        sb.append("</details>\n\n");
    }
}
