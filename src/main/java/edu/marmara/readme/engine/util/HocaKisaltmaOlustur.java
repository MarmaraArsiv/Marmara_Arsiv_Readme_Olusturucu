package edu.marmara.readme.engine.util;

import java.util.Collection;
import java.util.Locale;

/**
 * Yeni eklenen hoca için kısaltma önerisi. Var olan kısaltmalar hocalar.json'da saklanır ve değişmez;
 * bu sınıf yalnızca yeni hoca için, arşivdeki kurala uygun bir öneri üretir:
 * adın ve soyadın baş harfleri ("Buket Doğan" → "BD"); çakışırsa soyadın ilk harfleri ("Abdullah Bal" → "ABal").
 */
public final class HocaKisaltmaOlustur {

    private static final Locale TR = Locale.forLanguageTag("tr");

    private HocaKisaltmaOlustur() {
    }

    public static String oner(String adUnvanli, Collection<String> mevcutlar) {
        String[] parcalar = TurkceSira.unvansiz(adUnvanli.strip()).split("\\s+");
        if (parcalar.length == 0 || parcalar[0].isEmpty()) {
            return "";
        }
        StringBuilder on = new StringBuilder();
        for (int i = 0; i < parcalar.length - 1; i++) {
            on.append(parcalar[i].substring(0, 1).toUpperCase(TR));
        }
        String soyad = parcalar[parcalar.length - 1];
        String temel = on + soyad.substring(0, 1).toUpperCase(TR);
        if (!mevcutlar.contains(temel)) {
            return temel;
        }
        for (int n = 3; n <= soyad.length(); n++) {
            String aday = on + soyad.substring(0, 1).toUpperCase(TR) + soyad.substring(1, n);
            if (!mevcutlar.contains(aday)) {
                return aday;
            }
        }
        for (int sayi = 2; ; sayi++) {
            if (!mevcutlar.contains(temel + sayi)) {
                return temel + sayi;
            }
        }
    }
}
