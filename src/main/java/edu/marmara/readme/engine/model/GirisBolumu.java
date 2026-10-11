package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * giris.json kök yapısı: ana README'nin başı.
 * "katkida_bulunun" ve "sayac" README'ye olduğu gibi yazılan markdown/HTML parçalarıdır.
 * "icindekiler" elemanları hazır markdown linkleridir, ör. "[Dersler](#-dersler)".
 */
public class GirisBolumu extends JsonNesnesi {
    public String baslik = "";
    public String aciklama = "";
    /** "Geri Bildirimde Bulunun" başlığının altındaki paragraf; form linkleri konfigürasyondan gelir. */
    public String geri_bildirim_aciklamasi = "";
    /** "## 🤝 Katkıda Bulunun" bölümü (Video Rehberler dahil), sonundaki boş satırla birlikte. */
    public String katkida_bulunun = "";
    /** Görüntülenme sayacı HTML'i. Sayacın kimliği değişirse sayım sıfırlanır, bu yüzden sabit tutulur. */
    public String sayac = "";
    public List<String> icindekiler = new ArrayList<>();
}
