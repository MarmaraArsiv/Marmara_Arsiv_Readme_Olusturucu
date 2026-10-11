package edu.marmara.readme.engine.util;

import java.util.Locale;

/**
 * GitHub'ın başlıklara verdiği bağlantı adı (README içi linkler için).
 * Unicode küçük harf (İ → i̇), boşluk → "-", harf/rakam/işaret ile "-", "_" kalır, emoji silinir
 * ama emoji içindeki birleştirici U+200D korunur. Örn. "👨‍🏫 Doç. Dr. Buket Doğan" → "#‍-doç-dr-buket-doğan".
 * GitHub'da render edilmiş README'ye karşı doğrulanmış kuralların Java karşılığı.
 */
public final class GithubSlug {

    private GithubSlug() {
    }

    public static String slug(String baslik) {
        StringBuilder sb = new StringBuilder("#");
        baslik.strip().toLowerCase(Locale.ROOT).codePoints().forEach(c -> {
            if (c == ' ') {
                sb.append('-');
            } else if (c == '-' || c == '_' || c == 0x200D || harfRakamIsaret(c)) {
                sb.appendCodePoint(c);
            }
        });
        return sb.toString();
    }

    private static boolean harfRakamIsaret(int c) {
        return switch (Character.getType(c)) {
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
                 Character.MODIFIER_LETTER, Character.OTHER_LETTER,
                 Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER,
                 Character.NON_SPACING_MARK, Character.ENCLOSING_MARK, Character.COMBINING_SPACING_MARK -> true;
            default -> false;
        };
    }
}
