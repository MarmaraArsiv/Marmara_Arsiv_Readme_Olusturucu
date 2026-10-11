package edu.marmara.readme.engine.config;

import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Python konfigurasyon_json_kontrol.py'nin Java portu — eksik anahtarları onarır.
 *
 * Bilinçli sapmalar:
 * 1. Python'da "9 anahtardan biri bile okunamıyorsa HEPSİ hardcoded default'a döner"
 *    şeklinde bariz bir hata var (degiskenler.py import-time try/except'i tüm dict'i
 *    tek parça okuyor). Burada her anahtar KENDİ default'una BAĞIMSIZ düşer.
 * 2. Google Form/CSV linkleri henüz Marmara için oluşturulmadığından boş string
 *    ("") geçerli bir "henüz yapılandırılmadı" değeri sayılır — her çalıştırmada
 *    tekrar tekrar "onarılıp" dosyanın gereksiz yeniden yazılmasını önler. Sadece
 *    anahtar hiç yoksa (null) bir kere "" ile doldurulur.
 */
public final class KonfigurasyonJsonKontrol {

    private KonfigurasyonJsonKontrol() {
    }

    /** path'teki konfigurasyon.json'u okur, eksik alanları doldurur, değiştiyse geri yazar. */
    public static Konfigurasyon guncelle(Path path, Path gitRepoDizini) {
        Konfigurasyon k = JsonUtil.read(path, Konfigurasyon.class);
        boolean updated = false;
        if (k == null) {
            k = new Konfigurasyon();
            updated = true;
        }

        if (k.github_url == null || k.github_url.isEmpty()) {
            k.github_url = gitRepoUrlGetir(gitRepoDizini);
            updated = true;
        }
        if (k.hoca_yorumlama == null) {
            k.hoca_yorumlama = "";
            updated = true;
        }
        if (k.hoca_oylama == null) {
            k.hoca_oylama = "";
            updated = true;
        }
        if (k.ders_yorumlama == null) {
            k.ders_yorumlama = "";
            updated = true;
        }
        if (k.ders_oylama == null) {
            k.ders_oylama = "";
            updated = true;
        }
        if (k.ders_oylama_csv == null) {
            k.ders_oylama_csv = "";
            updated = true;
        }
        if (k.ders_yorumlama_csv == null) {
            k.ders_yorumlama_csv = "";
            updated = true;
        }
        if (k.hoca_oylama_csv == null) {
            k.hoca_oylama_csv = "";
            updated = true;
        }
        if (k.hoca_yorumlama_csv == null) {
            k.hoca_yorumlama_csv = "";
            updated = true;
        }
        if (k.dokumanlar_repo_yolu == null || k.dokumanlar_repo_yolu.isEmpty()) {
            k.dokumanlar_repo_yolu = "..";
            updated = true;
        }
        if (k.cikmislar == null) {
            k.cikmislar = "";
            updated = true;
        }

        if (!anahtarVar(path, "mufredat")) {
            // Eski dosyalarda "mufredat" yok: varsayılan kural (2026 geçişi) dosyaya yazılsın ki görünür ve düzenlenebilir olsun.
            updated = true;
        }

        if (updated) {
            JsonUtil.write(path, k);
        }
        return k;
    }

    private static boolean anahtarVar(Path path, String anahtar) {
        try {
            return java.nio.file.Files.exists(path)
                    && JsonUtil.MAPPER.readTree(java.nio.file.Files.readString(path, StandardCharsets.UTF_8)).has(anahtar);
        } catch (IOException e) {
            return false;
        }
    }

    /** repoDizini'nde `git config --get remote.origin.url` çalıştırır; başarısızsa açıklayıcı bir mesaj döner. */
    private static String gitRepoUrlGetir(Path repoDizini) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "-C", repoDizini.toString(),
                    "config", "--get", "remote.origin.url");
            pb.redirectErrorStream(false);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            int exit = p.waitFor();
            if (exit == 0 && !out.isEmpty()) {
                return out;
            }
            return "";
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }
}
