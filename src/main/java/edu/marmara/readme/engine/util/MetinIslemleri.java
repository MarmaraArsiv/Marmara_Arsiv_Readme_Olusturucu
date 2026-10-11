package edu.marmara.readme.engine.util;

/** Arayüzde uzun metinleri düğme ve ipucu metni olarak göstermek için kısaltma. */
public final class MetinIslemleri {

    private MetinIslemleri() {
    }

    /** Metni belirtilen maksimum uzunlukta sınırlar ve gerekirse "..." ile kısaltır (sondan kısaltma). */
    public static String kisaltMetin(String metin, int maksUzunluk) {
        if (metin == null) {
            return "";
        }
        if (metin.length() > maksUzunluk) {
            return metin.substring(0, maksUzunluk - 3) + "...";
        }
        return metin;
    }

    public static String kisaltMetin(String metin) {
        return kisaltMetin(metin, 70);
    }
}
