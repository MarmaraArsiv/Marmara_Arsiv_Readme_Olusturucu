package edu.marmara.readme.engine;

import java.util.List;
import java.util.Map;

/** Python degiskenler.py'deki ortak sabitlerin Java karşılığı — sadece motor için gerekenler. */
public final class Sabitler {
    private Sabitler() {
    }

    public static final String PROF_DR = "Prof. Dr.";
    public static final String DOC_DR = "Doç. Dr.";
    public static final String DR = "Dr.";
    /** Bilinçli düzeltme: orijinal YTÜ aracındaki sabit "Arş. Grv." yazım hatası içeriyor; doğrusu "Arş. Gör."dür. */
    public static final String ARS_GRV = "Arş. Gör.";
    public static final List<String> UNVANLAR = List.of(PROF_DR, DOC_DR, DR, ARS_GRV);

    /** Ana unvanla birlikte gelen ek unvan parçaları — kısaltma hesaplanırken bunlar da elenir. */
    public static final List<String> EK_UNVAN_PARCALARI = List.of("Öğretim Üyesi", "Öğr. Üyesi");

    public static final String GUZ = "Güz";
    public static final String BAHAR = "Bahar";
    public static final String MESLEKI_SECMELI_1 = "Mesleki Seçmeli 1";

    public static final String README_MD = "README.md";

    public static final String VARSAYILAN_HOCA_BOLUM_ADI = "Hocalar";
    public static final String VARSAYILAN_HOCA_BOLUM_ACIKLAMASI =
            "Bu bölüm, Marmara Üniversitesi Teknoloji Fakültesi Bilgisayar Mühendisliği bölümündeki hocaların "
            + "detaylı bilgilerini içerir. Hocaların adları, ofis bilgileri, araştırma sayfalarının bağlantıları "
            + "ve verdikleri bazı dersler bu bölümde listelenmektedir. Öğrenciler ve diğer ilgililer için hocalar "
            + "hakkında temel bilgiler ve iletişim detayları sunulmaktadır. Hocaların puanlamaları tamamen "
            + "subjektiftir ve 0-10 yıldız arasında yapılmıştır.";
    public static final String VARSAYILAN_HOCA_AKTIF_GOREVDE_DEGIL_MESAJI =
            "Bu hoca artık aktif görevde değil. Ya emekli olmuş ya da başka bir üniversiteye geçmiş olabilir.";

    public static final String VARSAYILAN_DERS_BOLUM_ADI = "Dersler";
    public static final String VARSAYILAN_DERS_BOLUM_ACIKLAMASI =
            "Bu bölümde, tüm dersler hakkında detaylı bilgiler ve kaynaklar bulunmaktadır. Öğrenciler bu bölümü "
            + "kullanarak ders materyallerine ve içeriklerine ulaşabilirler.";
    public static final String VARSAYILAN_GUNCEL_OLMAYAN_DERS_ACIKLAMASI =
            "Bu ders artık müfredata dahil değildir. Ya tamamen kaldırılmış, ya ismi ve içeriği güncellenmiş ya da birleştirilmiş olabilir.";
    public static final String VARSAYILAN_DERS_KLASORU_BULUNAMADI_MESAJI =
            "Henüz dersle alakalı bir döküman ne yazık ki yok. Katkıda bulunmak istersen lütfen bizimle iletişime geç...";

    public static final String FAYDALI_OLABILECEK_KAYNAKLAR_UYARI_MESAJI =
            "Kaynaklar öğrenciler tarafından oluşturulmuştur. Bundan dolayı içeriklerin doğruluğu garanti edilemez.";

    public static final String VARSAYILAN_KATKIDA_BULUNANLAR_BOLUM_ADI = "Katkıda Bulunanlar";
    public static final String VARSAYILAN_KATKIDA_BULUNANLAR_BOLUM_ACIKLAMASI =
            "Bu bölümde reponun hazırlanmasında katkıda bulunan insanlar listelenmiştir. Siz de katkıda bulunmak "
            + "isterseniz bizimle iletişime geçin. Ya da merge request gönderin.";

    public static final String VARSAYILAN_REPO_KULLANIMI_BOLUM_ADI = "Repo Kullanımı";

    public static final String LISANSUSTU = "Lisansüstü";
    public static final String MESLEKI_SECMELI = "Mesleki Seçmeli";
    public static final String ARTIK_MUFREDATA_DAHIL_OLMAYAN_DERSLER =
            "Artık Güncel Müfredata Dahil Olmayan Dersler";

    /** writers/yardimci.py: UNVAN_ONCELIKLERI — hoca sıralamasında ünvan önceliği (bulunamazsa 4). */
    public static final Map<String, Integer> UNVAN_ONCELIKLERI = Map.of("Prof.", 1, "Doç.", 2, "Dr.", 3);

    /** hocalar_writer.py: unvan grubu değiştiğinde yazılan alt başlıklar, UNVANLAR sırasıyla eşleşir. */
    public static final List<String> UNVAN_BASLIKLARI =
            List.of("Profesörler", "Doçentler", "Doktor Öğretim Üyeleri", "Araştırma Görevlileri");

    /** katkida_bulunanlar_writer.py: KATKIDA_BULUNMA_ORANI_DIZI (en çoktan en aza, sıralama ve varsayılan için). */
    public static final List<String> KATKIDA_BULUNMA_ORANI_DIZI =
            List.of("Çok", "Orta Üst", "Orta", "Orta Alt", "Az", "Çok Az");

    /** katkida_bulunanlar_writer.py: KATKIDA_EMOJILER — oran index'ine göre emoji (taç kaldırıldı, kullanıcı isteği). */
    public static final List<String> KATKIDA_EMOJILER = List.of("⭐", "🌟", "💫", "✨", "🔹", "");

    public static final String YOK = "yok";
    public static final String SECMELI = "Seçmeli";
    public static final List<String> DONEMLER_DIZISI_YOKLA_BERABER = List.of(YOK, GUZ, BAHAR);

    public static final List<String> DERS_TIPLERI = List.of(
            "Zorunlu", MESLEKI_SECMELI, "Mesleki Seçmeli 1", "Mesleki Seçmeli 2",
            "Üniversite Mesleki Seçmeli", SECMELI, "Sosyal Seçmeli 1",
            "Üniversite Sosyal Seçmeli", LISANSUSTU
    );

    /** Marmara'ya özgü: bilinen müfredat yılları (GUI'de seçilebilir varsayılan küme, serbestçe yeni eklenebilir). */
    public static final List<String> BILINEN_MUFREDATLAR = new java.util.ArrayList<>(List.of("2021", "2026"));

    public static final String VARSAYILAN_GIRIS_BASLIK =
            "Marmara Üniversitesi Teknoloji Fakültesi Bilgisayar Mühendisliği Ders Notları";
    public static final String VARSAYILAN_GIRIS_ACIKLAMA =
            "Bu repository, Marmara Üniversitesi Teknoloji Fakültesi Bilgisayar Mühendisliği bölümünde verilen "
            + "derslerin notlarını, örnek sorularını ve ilgili kaynakları barındırmaktadır. Öğrencilerin dersleri "
            + "daha etkin bir şekilde öğrenmelerini desteklemek amacıyla hazırlanmıştır.";
}
