package edu.marmara.readme.engine;

import java.util.List;

/** Motorun ve arayüzün ortak sabitleri. */
public final class Sabitler {

    private Sabitler() {
    }

    public static final String README_MD = "README.md";

    public static final String VARSAYILAN_GIRIS_BASLIK = "Marmara Bilgisayar Mühendisliği Arşiv";
    public static final String VARSAYILAN_GIRIS_ACIKLAMA =
            "Marmara Üniversitesi Teknoloji Fakültesi Bilgisayar Mühendisliği öğrencileri için akademik arşiv.";
    public static final String VARSAYILAN_REPO_KULLANIMI_BOLUM_ADI = "Repo Kullanımı";
    public static final String VARSAYILAN_KATKIDA_BULUNANLAR_BOLUM_ADI = "Katkıda Bulunanlar";
    public static final String VARSAYILAN_KATKIDA_BULUNANLAR_BOLUM_ACIKLAMASI =
            "Bu bölümde reponun hazırlanmasında katkıda bulunan insanlar listelenmiştir.";

    /**
     * Katkı seviyeleri, en çoktan en aza. GitHub'daki katkida_bulunanlari_guncelle.py betiğindeki
     * ORAN_DIZI ile aynı olmalı; seviye README'deki başlık boyutunu (h1, h2, …) belirler.
     */
    public static final List<String> KATKIDA_BULUNMA_ORANI_DIZI =
            List.of("Çok", "Orta Üst", "Orta", "Orta Alt", "Az", "Çok Az");

    /** Seviyeye göre adın iki yanındaki emoji (betikteki EMOJILER ile aynı). */
    public static final List<String> KATKIDA_EMOJILER = List.of("⭐", "🌟", "💫", "✨", "🔹", "");
}
