package edu.marmara.readme.engine.util;

import java.nio.charset.StandardCharsets;

/**
 * Python urllib.parse.quote(metin) ile aynı yüzde kodlaması: harf, rakam, "_.-~" ve "/" olduğu gibi
 * kalır, gerisi UTF-8 baytları olarak %XX (büyük harf). Boşluk "+" değil "%20" olur.
 * README'deki klasör linkleri ve önceden doldurulmuş form linkleri bu kodlamayı kullanır.
 */
public final class UrlKodla {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private UrlKodla() {
    }

    public static String kodla(String metin) {
        StringBuilder sb = new StringBuilder();
        for (byte b : metin.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-' || c == '~' || c == '/') {
                sb.append((char) c);
            } else {
                sb.append('%').append(HEX[c >> 4]).append(HEX[c & 0xF]);
            }
        }
        return sb.toString();
    }
}
