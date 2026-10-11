package edu.marmara.readme.engine.util;

import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * Türkçe alfabeye göre sıralama (hoca listeleri ve kısaltmalar). Arşivde README'leri önceden
 * üreten Python betiğiyle birebir aynı anahtarı kullanır: I→ı, İ→i, küçük harf; alfabedeki harfler
 * sırasıyla, boşluk en önce, alfabe dışı karakterler alfabeden sonra kod noktasına göre.
 */
public final class TurkceSira {

    private static final String ALFABE = "abcçdefgğhıijklmnoöprsştuüvyz";
    private static final Pattern UNVAN = Pattern.compile(
            "^(Prof\\. Dr\\.|Prof\\.|Doç\\. Dr\\.|Dr\\. Öğretim Üyesi|Öğr\\. Gör\\. Dr\\.|Öğr\\. Gör\\."
                    + "|Arş\\. Gör\\. Dr\\.|Arş\\. Gör\\.|Dr\\.)\\s*");

    /** Metinleri Türkçe alfabeye göre karşılaştırır. */
    public static final Comparator<String> KARSILASTIRICI = TurkceSira::karsilastir;

    private TurkceSira() {
    }

    public static int karsilastir(String a, String b) {
        int[] x = anahtar(a);
        int[] y = anahtar(b);
        for (int i = 0; i < Math.min(x.length, y.length); i++) {
            if (x[i] != y[i]) {
                return Integer.compare(x[i], y[i]);
            }
        }
        return Integer.compare(x.length, y.length);
    }

    private static int[] anahtar(String s) {
        String k = s.replace("I", "ı").replace("İ", "i").toLowerCase(java.util.Locale.ROOT);
        return k.codePoints().map(c -> {
            if (c == ' ') {
                return -1;
            }
            int i = ALFABE.indexOf(c);
            return i >= 0 ? i : 100 + c;
        }).toArray();
    }

    /** "Prof. Dr. Ad Soyad" → "Ad Soyad" (hocalar grup içinde unvansız adlarına göre dizilir). */
    public static String unvansiz(String ad) {
        return UNVAN.matcher(ad).replaceFirst("");
    }
}
