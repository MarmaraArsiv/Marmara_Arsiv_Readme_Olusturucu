package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.util.TurkceSira;
import edu.marmara.readme.engine.util.UrlKodla;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mevcut README'lerden GitHub'ın (form_linkleri.py, readme_gorselleri.py) doldurduğu blokları okur.
 * Araç README'leri yeniden yazarken bu blokları aynen geri koyar; böylece yıldızlar, onaylı yorumlar
 * ve doluluk tablosu kaybolmaz. Blok henüz yoksa (yeni ders/hoca) yazıcı varsayılan hâlini yazar,
 * GitHub ilk push'ta doldurur.
 */
public final class KorunanBloklar {

    private static final Pattern DERS_KARTI = Pattern.compile("(?m)^#### 📘 .+ \n");
    private static final Pattern HOCA_KARTI = Pattern.compile("(?m)^#### \\S+ (.+) \n");
    private static final Pattern DONEM_DERSI = Pattern.compile("(?m)^### 📘 (.+?)\\s*$");
    private static final Pattern KLASOR_LINKI = Pattern.compile("📂 \\[Ders Klasörü\\]\\(\\./([^)]*)\\)");

    private final Map<String, String> kokYildiz = new HashMap<>();
    private final Map<String, String> kokGorus = new HashMap<>();
    // Hoca blokları ünvansız ada göre tutulur: terfi eden hocanın adı güncellenince yorumları kaybolmaz.
    private final Map<String, String> hocaKartAdi = new HashMap<>();
    private final Map<String, String> hocaGorus = new HashMap<>();
    private final Map<String, List<String>> hocaYorumlari = new HashMap<>();
    private final Map<String, String> donemYildiz = new HashMap<>();
    private final Map<String, String> donemGorus = new HashMap<>();
    private final Map<String, String> dersYildiz = new HashMap<>();
    private final Map<String, String> dersGorus = new HashMap<>();
    private String doluluk;

    private KorunanBloklar() {
    }

    /** repo: arşivin kökü; donemKlasorleri: "1-1" … "4-2"; dersKlasorleri: tüm derslerin klasörleri. */
    public static KorunanBloklar oku(Path repo, List<String> donemKlasorleri, List<String> dersKlasorleri) {
        KorunanBloklar k = new KorunanBloklar();
        String kok = oku(repo.resolve(Sabitler.README_MD));
        if (kok != null) {
            k.kokuOku(kok);
        }
        for (String donem : donemKlasorleri) {
            String t = oku(repo.resolve(donem).resolve(Sabitler.README_MD));
            if (t != null) {
                k.donemiOku(donem, t);
            }
        }
        for (String klasor : dersKlasorleri) {
            String t = oku(repo.resolve(klasor).resolve(Sabitler.README_MD));
            if (t != null) {
                String y = blokBul(t, "- ⭐ **Yıldız Sayıları:**");
                String g = blokBul(t, "- 💬 **Öğrenci Görüşleri:**");
                if (y != null) {
                    k.dersYildiz.put(klasor, y);
                }
                if (g != null) {
                    k.dersGorus.put(klasor, g);
                }
            }
        }
        return k;
    }

    private void kokuOku(String t) {
        Matcher m = DERS_KARTI.matcher(t);
        while (m.find()) {
            String kart = kartGovdesi(t, m.end(), "  ");
            Matcher km = KLASOR_LINKI.matcher(kart);
            if (!km.find()) {
                continue;
            }
            String klasor = URLDecoder.decode(km.group(1), StandardCharsets.UTF_8);
            String y = blokBul(kart, "  - ⭐ **Yıldız Sayıları:**");
            String g = blokBul(kart, "  - 💬 **Öğrenci Görüşleri:**");
            if (y != null) {
                kokYildiz.put(klasor, y);
            }
            if (g != null) {
                kokGorus.put(klasor, g);
            }
        }
        int hocalar = t.indexOf("## 🎓 ");
        if (hocalar >= 0) {
            Matcher h = HOCA_KARTI.matcher(t);
            h.region(hocalar, t.length());
            while (h.find()) {
                String kart = kartGovdesi(t, h.end(), "");
                String g = blokBul(kart, "- 💬 **Öğrenci Görüşleri:**");
                if (g == null) {
                    continue;
                }
                String anahtar = TurkceSira.unvansiz(h.group(1));
                hocaKartAdi.put(anahtar, h.group(1));
                hocaGorus.put(anahtar, g);
                List<String> yorumlar = new ArrayList<>();
                for (String satir : g.split("\n")) {
                    if (satir.startsWith("  - 👤 ")) {
                        yorumlar.add(satir.substring("  - ".length()));
                    }
                }
                hocaYorumlari.put(anahtar, yorumlar);
            }
        }
        int bas = t.indexOf("<!-- DOLULUK_BASLANGIC -->");
        int son = t.indexOf("<!-- DOLULUK_BITIS -->");
        if (bas >= 0 && son > bas) {
            doluluk = t.substring(bas, son + "<!-- DOLULUK_BITIS -->".length()) + "\n";
        }
    }

    private void donemiOku(String donem, String t) {
        Matcher m = DONEM_DERSI.matcher(t);
        List<int[]> konumlar = new ArrayList<>();
        List<String> adlar = new ArrayList<>();
        while (m.find()) {
            konumlar.add(new int[]{m.start(), m.end()});
            adlar.add(m.group(1));
        }
        for (int i = 0; i < konumlar.size(); i++) {
            int son = i + 1 < konumlar.size() ? konumlar.get(i + 1)[0] : t.length();
            String parca = t.substring(konumlar.get(i)[1], son);
            String anahtar = donem + "/" + adlar.get(i);
            String y = blokBul(parca, "- ⭐ **Yıldız Sayıları:**");
            String g = blokBul(parca, "- 💬 **Öğrenci Görüşleri:**");
            if (y != null) {
                donemYildiz.put(anahtar, y);
            }
            if (g != null) {
                donemGorus.put(anahtar, g);
            }
        }
    }

    /** Kart başlığından sonra, verilen girintiyle başlayan satırlar bitene kadarki gövde. */
    private static String kartGovdesi(String t, int bas, String girinti) {
        int son = bas;
        while (son < t.length()) {
            int ss = t.indexOf('\n', son);
            if (ss < 0) {
                ss = t.length();
            }
            String satir = t.substring(son, ss);
            if (satir.isEmpty() || !(satir.startsWith(girinti + "- ") || satir.startsWith(girinti + " "))) {
                break;
            }
            son = Math.min(ss + 1, t.length());
        }
        return t.substring(bas, son);
    }

    /**
     * Metinde tam olarak "baslik" satırıyla başlayan bloğu (başlık + ondan daha içeride olan alt satırlar)
     * döner, satır sonlarıyla birlikte; yoksa null. GitHub betiğindeki blok_sonu kuralının aynısı.
     */
    static String blokBul(String metin, String baslik) {
        int bas = -1;
        int ara = 0;
        while ((ara = metin.indexOf(baslik, ara)) >= 0) {
            boolean satirBasi = ara == 0 || metin.charAt(ara - 1) == '\n';
            int ss = metin.indexOf('\n', ara);
            boolean satirSonu = ss == ara + baslik.length();
            if (satirBasi && satirSonu) {
                bas = ara;
                break;
            }
            ara += baslik.length();
        }
        if (bas < 0) {
            return null;
        }
        int girinti = girinti(baslik);
        int son = metin.indexOf('\n', bas) + 1;
        while (son < metin.length()) {
            int ss = metin.indexOf('\n', son);
            if (ss < 0) {
                ss = metin.length();
            }
            String satir = metin.substring(son, ss);
            if (satir.isBlank() || girinti(satir) <= girinti) {
                break;
            }
            son = Math.min(ss + 1, metin.length());
        }
        String blok = metin.substring(bas, son);
        return blok.endsWith("\n") ? blok : blok + "\n";
    }

    private static int girinti(String satir) {
        int i = 0;
        while (i < satir.length() && satir.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private static String oku(Path yol) {
        try {
            return Files.exists(yol) ? Files.readString(yol, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new RuntimeException("README okunamadı: " + yol, e);
        }
    }

    public String kokYildiz(String klasor) {
        return kokYildiz.get(klasor);
    }

    public String kokGorus(String klasor) {
        return kokGorus.get(klasor);
    }

    /** Hocanın kartındaki yorum bloğu; ünvanı değişmişse yorum linkindeki ad yeni ada çevrilir. */
    public String hocaGorus(String hoca) {
        String anahtar = TurkceSira.unvansiz(hoca);
        String g = hocaGorus.get(anahtar);
        String eskiAd = hocaKartAdi.get(anahtar);
        return g == null || eskiAd.equals(hoca) ? g : g.replace(UrlKodla.kodla(eskiAd), UrlKodla.kodla(hoca));
    }

    /** Hocanın ana README kartındaki onaylı yorum satırları ("👤 **_mahlas_**: …"), en yeni üstte. */
    public List<String> hocaYorumlari(String hoca) {
        return hocaYorumlari.getOrDefault(TurkceSira.unvansiz(hoca), List.of());
    }

    public String donemYildiz(String donem, String ders) {
        return donemYildiz.get(donem + "/" + ders);
    }

    public String donemGorus(String donem, String ders) {
        return donemGorus.get(donem + "/" + ders);
    }

    public String dersYildiz(String klasor) {
        return dersYildiz.get(klasor);
    }

    public String dersGorus(String klasor) {
        return dersGorus.get(klasor);
    }

    /** "<!-- DOLULUK_BASLANGIC -->" … "<!-- DOLULUK_BITIS -->\n"; README'de yoksa null. */
    public String doluluk() {
        return doluluk;
    }
}
