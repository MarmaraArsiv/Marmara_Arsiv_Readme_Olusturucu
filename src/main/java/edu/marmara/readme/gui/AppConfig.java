package edu.marmara.readme.gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Kullanıcı tercihleri (java.util.prefs, OS'e göre registry/plist/dosya): son açılan arşiv ve son açılan
 * arşivlerin listesi. Araç birden fazla bölüm arşivini düzenleyebilir; açılışta son kullanılan açılır.
 */
public final class AppConfig {

    private static final String PREF_KEY = "json_depo_yolu";
    private static final String SON_ARSIVLER = "son_arsivler";
    private static final int SON_ARSIV_SAYISI = 10;
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppConfig.class);

    private AppConfig() {
    }

    public static Path jsonDeposu() {
        String kayitli = PREFS.get(PREF_KEY, null);
        if (kayitli != null) {
            return Path.of(kayitli);
        }
        return Path.of(".");
    }

    /** Açılan arşivi son kullanılan olarak kaydeder ve son arşivler listesinin başına alır. */
    public static void jsonDeposunuAyarla(Path yol) {
        String yeni = yol.toAbsolutePath().normalize().toString();
        PREFS.put(PREF_KEY, yeni);
        List<String> liste = new ArrayList<>(List.of(yeni));
        for (Path p : sonArsivler()) {
            if (!p.toString().equals(yeni) && liste.size() < SON_ARSIV_SAYISI) {
                liste.add(p.toString());
            }
        }
        PREFS.put(SON_ARSIVLER, String.join("\n", liste));
    }

    /** Son açılan arşivlerin JSON klasörleri (en yeni önce); artık diskte olmayanlar atlanır. */
    public static List<Path> sonArsivler() {
        List<Path> liste = new ArrayList<>();
        for (String s : PREFS.get(SON_ARSIVLER, "").split("\n")) {
            if (!s.isBlank() && Files.isDirectory(Path.of(s))) {
                liste.add(Path.of(s));
            }
        }
        return liste;
    }
}
