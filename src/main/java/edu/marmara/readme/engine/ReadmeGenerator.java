package edu.marmara.readme.engine;

import edu.marmara.readme.engine.config.KonfigurasyonJsonKontrol;
import edu.marmara.readme.engine.model.*;
import edu.marmara.readme.engine.util.FolderCache;
import edu.marmara.readme.engine.util.GithubMetinIslemleri;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
import edu.marmara.readme.engine.writer.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Python readme_olustur.py'nin (ReadmeGenerator sınıfının) Java portu.
 *
 * JSON dosyalarını okur, kök README.md'yi ve her ders/dönem klasöründeki
 * README.md'leri üretir. Konsol çıktısı yerine opsiyonel bir {@code ilerlemeDinleyicisi}
 * callback'i kullanılır (GUI ilerleme çubuğuna bağlanabilir, Python'daki
 * custom_write/_progress_callback karşılığı).
 */
public class ReadmeGenerator {

    public static final String GIRIS_JSON = "giris.json";
    public static final String REPO_KULLANIMI_JSON = "repo_kullanimi.json";
    public static final String DERSLER_JSON = "dersler.json";
    public static final String HOCALAR_JSON = "hocalar.json";
    public static final String KATKIDA_BULUNANLAR_JSON = "katkida_bulunanlar.json";
    public static final String DONEMLER_JSON = "donemler.json";
    public static final String KONFIGURASYON_JSON = "konfigurasyon.json";
    public static final String MAAS_ISTATISTIKLERI_TXT = "maas_istatistikleri.txt";

    private final Path jsonDeposu;
    private final Consumer<String> ilerlemeDinleyicisi;

    private GirisBolumu giris;
    private RepoKullanimiBolumu repoKullanimi;
    private DerslerBolumu dersler;
    private HocalarBolumu hocalar;
    private KatkidaBulunanlarBolumu katkidaBulunanlar;
    private DonemlerBolumu donemler;
    private Konfigurasyon konfig;
    private String maasIstatistikleri;

    private Path dokumanlarRepoYolu;
    private Path anaReadmeYolu;
    private FolderCache folderCache;

    private final Map<Ders, Path> dersKlasorleri = new IdentityHashMap<>();

    public ReadmeGenerator(Path jsonDeposu, Consumer<String> ilerlemeDinleyicisi) {
        this.jsonDeposu = jsonDeposu;
        this.ilerlemeDinleyicisi = ilerlemeDinleyicisi != null ? ilerlemeDinleyicisi : (s -> {
        });
    }

    private void log(String mesaj) {
        ilerlemeDinleyicisi.accept(mesaj);
    }

    private Path jsonYolu(String dosyaAdi) {
        return jsonDeposu.resolve(dosyaAdi);
    }

    private void verileriYukle() {
        log("Veriler yükleniyor...");
        konfig = KonfigurasyonJsonKontrol.guncelle(jsonYolu(KONFIGURASYON_JSON), jsonDeposu);
        dokumanlarRepoYolu = jsonDeposu.resolve(konfig.dokumanlar_repo_yolu).normalize();
        anaReadmeYolu = dokumanlarRepoYolu.resolve(Sabitler.README_MD);

        giris = JsonUtil.read(jsonYolu(GIRIS_JSON), GirisBolumu.class);
        repoKullanimi = JsonUtil.read(jsonYolu(REPO_KULLANIMI_JSON), RepoKullanimiBolumu.class);
        dersler = JsonUtil.read(jsonYolu(DERSLER_JSON), DerslerBolumu.class);
        hocalar = JsonUtil.read(jsonYolu(HOCALAR_JSON), HocalarBolumu.class);
        katkidaBulunanlar = JsonUtil.read(jsonYolu(KATKIDA_BULUNANLAR_JSON), KatkidaBulunanlarBolumu.class);
        donemler = JsonUtil.read(jsonYolu(DONEMLER_JSON), DonemlerBolumu.class);

        Path maasYolu = jsonYolu(MAAS_ISTATISTIKLERI_TXT);
        if (Files.exists(maasYolu)) {
            try {
                maasIstatistikleri = Files.readString(maasYolu, StandardCharsets.UTF_8);
            } catch (IOException e) {
                maasIstatistikleri = null;
            }
        }

        if (dersler != null) {
            for (Ders d : dersler.dersler) {
                d.ad = MetinIslemleri.dersAdiNormalize(d.ad);
            }
            dersler.dersler.sort(ReadmeGenerator::dersSiralama);
        }
    }

    /** yil_sirasi=[1,2,3,4,0], donem_sirasi=["Güz","Bahar",""] önceliğiyle, sonra ada göre sırala. */
    private static int dersSiralama(Ders a, Ders b) {
        List<Integer> yilSirasi = List.of(1, 2, 3, 4, 0);
        List<String> donemSirasi = List.of(Sabitler.GUZ, Sabitler.BAHAR, "");
        int ia = yilSirasi.indexOf(a.yil);
        int ib = yilSirasi.indexOf(b.yil);
        if (ia != ib) {
            return Integer.compare(ia < 0 ? yilSirasi.size() : ia, ib < 0 ? yilSirasi.size() : ib);
        }
        int da = donemSirasi.indexOf(a.donem == null ? "" : a.donem);
        int db = donemSirasi.indexOf(b.donem == null ? "" : b.donem);
        if (da != db) {
            return Integer.compare(da < 0 ? donemSirasi.size() : da, db < 0 ? donemSirasi.size() : db);
        }
        return (a.ad == null ? "" : a.ad).toLowerCase(Locale.ROOT)
                .compareTo((b.ad == null ? "" : b.ad).toLowerCase(Locale.ROOT));
    }

    private void cacheOlustur() {
        log("Klasör cache oluşturuluyor...");
        folderCache = new FolderCache(dokumanlarRepoYolu, 4);
        log("Cache'de " + folderCache.folderCount() + " klasör bulundu.");
    }

    public void generateAnaReadme() {
        log("Ana README.md oluşturuluyor...");
        try {
            Files.deleteIfExists(anaReadmeYolu);
        } catch (IOException ignored) {
        }

        BufferedReadmeWriter writer = new BufferedReadmeWriter();

        if (giris != null) {
            log("Giriş bilgileri ekleniyor...");
            new GirisWriter().write(writer, giris, konfig);
        }
        if (repoKullanimi != null) {
            log("Repo kullanımı ekleniyor...");
            new RepoKullanimiWriter().write(writer, repoKullanimi);
        }
        if (maasIstatistikleri != null) {
            log("Maaş istatistikleri ekleniyor...");
            yazMaasIstatistikleri(writer, maasIstatistikleri);
        }
        if (dersler != null) {
            log("Ders bilgileri ekleniyor...");
            new DerslerWriter(folderCache, hocalar, dokumanlarRepoYolu).write(writer, dersler, konfig);
        }
        if (hocalar != null) {
            log("Hoca bilgileri ekleniyor...");
            new HocalarWriter(dersler).write(writer, hocalar, konfig);
        }
        if (hocalar != null) {
            log("Hoca kısaltmaları ekleniyor...");
            yazHocaKisaltmalari(writer, hocalar);
        }
        if (katkidaBulunanlar != null) {
            log("Katkıda bulunanlar ekleniyor...");
            new KatkidaBulunanlarWriter().write(writer, katkidaBulunanlar);
        }
        log("Yıldız geçmişi ekleniyor...");
        yazYildizGecmisi(writer, konfig.github_url);

        writer.save(anaReadmeYolu);
        log("Ana README.md oluşturuldu.");
    }

    private void yazHocaKisaltmalari(BufferedReadmeWriter writer, HocalarBolumu data) {
        Map<String, String> kisaltmalar = new TreeMap<>();
        List<String> adlar = data.hocalar.stream()
                .filter(h -> h.ad != null && !h.ad.isEmpty())
                .map(h -> h.ad).toList();
        Map<String, String> adToKisaltma = edu.marmara.readme.engine.util.HocaKisaltmaOlustur.benzersizKisaltmalarUret(adlar);
        for (Hoca hoca : data.hocalar) {
            if (hoca.ad != null && !hoca.ad.isEmpty()) {
                String kisaltma = adToKisaltma.get(hoca.ad);
                if (kisaltma != null) {
                    kisaltmalar.put(kisaltma, hoca.ad);
                }
            }
        }
        writer.writeline("<details>");
        writer.writeline("<summary><b>🆎 Hoca Kısaltmaları</b></summary>\n");
        writer.writeline("<h2 align='center'>🆎 Hoca Kısaltmaları</h2>\n");
        for (Map.Entry<String, String> e : kisaltmalar.entrySet()) {
            writer.writeline("<p align='center'>🔹 <b>" + e.getKey() + "</b> &emsp; " + e.getValue() + " 🔹</p>");
        }
        writer.writeline("</details>\n");
    }

    private void yazYildizGecmisi(BufferedReadmeWriter writer, String githubUrl) {
        String yildizUrl = githubUrl != null && githubUrl.contains("github.com/")
                ? githubUrl.substring(githubUrl.indexOf("github.com/") + "github.com/".length())
                : "";
        writer.write("\n## Yıldız Geçmişi\n[![Star History Chart](https://api.star-history.com/svg?repos="
                + yildizUrl + "&type=Date)](https://star-history.com/#" + yildizUrl + "&Date)\n");
    }

    private void yazMaasIstatistikleri(BufferedReadmeWriter writer, String veri) {
        writer.writeline("<details>");
        writer.writeline("<summary><b>💰 Bölüm Mezunları Maaş İstatistikleri</b></summary>\n");
        writer.writeline("<h2 align='center'>💰 Bölüm Mezunları Maaş İstatistikleri</h2>\n");
        writer.write(veri);
        writer.writeline("\n</details>\n");
    }

    public void generateDersReadmeleri() {
        if (dersler == null) {
            log("HATA: Ders bilgileri bulunamadı.");
            return;
        }
        log("Ders README'leri oluşturuluyor...");
        DersKlasorWriter dersKlasorWriter = new DersKlasorWriter(dersler, dokumanlarRepoYolu);

        for (Ders ders : dersler.dersler) {
            log(ders.ad + " README.md oluşturuluyor...");

            Optional<Path> bulunan = folderCache.findBestMatch(ders.ad);
            Path dersKlasoru;
            boolean klasorSonradanOlustu;

            if (bulunan.isPresent()) {
                dersKlasoru = bulunan.get();
                klasorSonradanOlustu = klasorBoşMu(dersKlasoru);
            } else {
                dersKlasoru = dersKlasoruOlustur(ders);
                klasorSonradanOlustu = true;
            }

            dersKlasorleri.put(ders, dersKlasoru);

            BufferedReadmeWriter writer = new BufferedReadmeWriter();
            dersKlasorWriter.writeDersReadme(writer, ders, klasorSonradanOlustu, dersKlasoru, konfig);
            writer.save(dersKlasoru.resolve(Sabitler.README_MD));

            log(ders.ad + " README.md oluşturuldu.");
        }
    }

    private boolean klasorBoşMu(Path klasor) {
        try (var stream = Files.list(klasor)) {
            return stream.noneMatch(p -> !p.getFileName().toString().equalsIgnoreCase(Sabitler.README_MD));
        } catch (IOException e) {
            return false;
        }
    }

    private Path dersKlasoruOlustur(Ders ders) {
        Donem sozdeDonem = new Donem();
        if (ders.yil != 0 && ders.donem != null && !ders.donem.isEmpty()) {
            sozdeDonem.yil = ders.yil;
            sozdeDonem.donem = ders.donem;
        } else if (ders.tip != null && !ders.tip.isEmpty()) {
            sozdeDonem.donem_adi = ders.tip;
        }
        Path donemYolu = MetinIslemleri.donemDosyaYoluGetir(sozdeDonem, dokumanlarRepoYolu.toString());
        Path dersKlasoru = donemYolu.resolve(ders.ad == null ? "" : ders.ad);
        try {
            Files.createDirectories(dersKlasoru);
        } catch (IOException e) {
            throw new RuntimeException("Ders klasörü oluşturulamadı: " + dersKlasoru, e);
        }
        return dersKlasoru;
    }

    public void generateDonemReadmeleri() {
        if (donemler == null) {
            log("HATA: Dönem bilgileri bulunamadı.");
            return;
        }
        log("Dönem README'leri oluşturuluyor...");
        DonemWriter donemWriter = new DonemWriter(dokumanlarRepoYolu);

        for (Donem donem : donemler.donemler) {
            log(donem.donem_adi + " README.md oluşturuluyor...");
            Path donemYolu = MetinIslemleri.donemDosyaYoluGetir(donem, dokumanlarRepoYolu.toString());
            try {
                Files.createDirectories(donemYolu);
            } catch (IOException e) {
                throw new RuntimeException("Dönem klasörü oluşturulamadı: " + donemYolu, e);
            }
            BufferedReadmeWriter writer = new BufferedReadmeWriter();
            donemWriter.writeDonemReadme(writer, donem);
            writer.save(donemYolu.resolve(Sabitler.README_MD));
            log(donem.donem_adi + " README.md oluşturuldu.");
        }

        if (dersler != null) {
            log("Dersler dönem README'lerine ekleniyor...");
            birlestirDerslerIleDonemler(donemWriter);
        }
    }

    private void birlestirDerslerIleDonemler(DonemWriter donemWriter) {
        String guncelOlmayanAciklama = dersler.guncel_olmayan_ders_aciklamasi;

        for (Ders ders : dersler.dersler) {
            log(ders.ad + " dönemine ekleniyor...");
            for (Donem donem : donemler.donemler) {
                if (!dersDonemeAitMi(ders, donem)) {
                    continue;
                }
                Path donemKlasoru = MetinIslemleri.donemDosyaYoluGetir(donem, dokumanlarRepoYolu.toString());
                Path dosyaYolu = donemKlasoru.resolve(Sabitler.README_MD);

                Path dersKlasoru = dersKlasorleri.get(ders);
                if (dersKlasoru == null) {
                    dersKlasoru = folderCache.findBestMatch(ders.ad).orElse(null);
                }

                BufferedReadmeWriter writer = new BufferedReadmeWriter();
                donemWriter.writeDersToDonem(writer, ders, guncelOlmayanAciklama, donemKlasoru, dersKlasoru, konfig);
                writer.appendToFile(dosyaYolu);
                break;
            }
            log(ders.ad + " dönemine eklendi.");
        }
    }

    private boolean dersDonemeAitMi(Ders ders, Donem donem) {
        if (ders.tip != null && ders.tip.equals(donem.donem_adi)) {
            return true;
        }
        return ders.yil == donem.yil
                && Objects.equals(ders.donem, donem.donem)
                && ders.yil != 0
                && ders.donem != null && !ders.donem.isEmpty();
    }

    public void generateAll() {
        long start = System.currentTimeMillis();
        verileriYukle();
        cacheOlustur();
        generateAnaReadme();
        generateDersReadmeleri();
        generateDonemReadmeleri();
        double saniye = (System.currentTimeMillis() - start) / 1000.0;
        log(String.format(Locale.ROOT, "%nTüm README dosyaları %.2f saniyede oluşturuldu.%n", saniye));
    }
}
