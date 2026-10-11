package edu.marmara.readme.cli;

import edu.marmara.readme.engine.ArsivSablonu;
import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.MufredatAyari;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Arayüzsüz çalıştırma. Yol olarak arşiv reposunun kökü ya da JSON klasörü verilebilir.
 * <pre>
 *   ReadmeOlusturCli &lt;arşiv&gt;                    README'leri JSON'dan üretip değişenleri yazar
 *   ReadmeOlusturCli &lt;arşiv&gt; --kontrol          hiçbir şey yazmaz; JSON ile README'ler uyuşmuyorsa
 *                                                farklı dosyaları listeler ve 1 koduyla çıkar
 *   ReadmeOlusturCli &lt;arşiv&gt; --altyapi-kontrol  GitHub betikleri/iş akışları aracın şablonundan farklıysa
 *                                                listeler ve 1 koduyla çıkar
 *   ReadmeOlusturCli &lt;arşiv&gt; --altyapi-guncelle bunları şablonla eşitler
 *   ReadmeOlusturCli --yeni-arsiv &lt;klasör&gt; --bolum "Elektrik-Elektronik Mühendisliği"
 *       --fakulte "Teknoloji Fakültesi" --github https://github.com/MarmaraArsiv/tek_elektrik
 *       [--sinif 4] [--havuz "Mesleki Seçmeli"]... [--mufredat "Güncel Müfredat"]... [--drive "Elektrik-Teknoloji"]
 *                                                yeni bölüm arşivini şablondan kurar
 * </pre>
 */
public final class ReadmeOlusturCli {

    private ReadmeOlusturCli() {
    }

    public static void main(String[] args) {
        try {
            System.exit(calistir(args));
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    static int calistir(String[] args) {
        List<String> a = List.of(args);
        if (a.contains("--yeni-arsiv")) {
            return yeniArsiv(a);
        }
        Path verilen = args.length > 0 && !args[0].startsWith("--") ? Path.of(args[0]) : null;
        Path jsonDeposu = verilen != null ? ReadmeGenerator.jsonKlasoruBul(verilen) : tekKomsuArsiv();
        if (jsonDeposu == null) {
            throw new IllegalStateException(verilen.toAbsolutePath() + " altında JSON klasörü bulunamadı "
                    + "(arşiv reposunun kökünü ya da .github/json_dosyalari klasörünü verin).");
        }
        if (a.contains("--altyapi-kontrol") || a.contains("--altyapi-guncelle")) {
            boolean yaz = a.contains("--altyapi-guncelle");
            List<Path> farkli = ArsivSablonu.altyapiyiGuncelle(ReadmeGenerator.repoKokuBul(jsonDeposu), yaz);
            if (farkli.isEmpty()) {
                System.out.println("Altyapı dosyaları aracın şablonuyla aynı.");
                return 0;
            }
            System.out.println(farkli.size() + (yaz ? " altyapı dosyası güncellendi:" : " altyapı dosyası şablondan farklı:"));
            farkli.forEach(p -> System.out.println("  " + p));
            return yaz ? 0 : 1;
        }
        if (!a.contains("--kontrol")) {
            new ReadmeGenerator(jsonDeposu, System.out::println).generateAll();
            return 0;
        }
        List<Path> farkli = new ReadmeGenerator(jsonDeposu, s -> {
        }).farkliOlanlar();
        if (farkli.isEmpty()) {
            System.out.println("README'ler JSON ile uyumlu.");
            return 0;
        }
        System.out.println(farkli.size() + " README JSON'dan üretilenle aynı değil:");
        farkli.forEach(p -> System.out.println("  " + p));
        return 1;
    }

    /** Arşiv verilmediyse: bulunulan klasördeki ya da yanındaki klasörlerdeki tek arşiv. */
    private static Path tekKomsuArsiv() {
        Path burasi = ReadmeGenerator.jsonKlasoruBul(Path.of("."));
        if (burasi != null) {
            return burasi;
        }
        List<Path> komsular = ReadmeGenerator.komsuArsivler(Path.of(".."));
        if (komsular.size() == 1) {
            return komsular.get(0);
        }
        throw new IllegalStateException(komsular.isEmpty()
                ? "Arşiv bulunamadı; arşiv reposunun klasörünü verin."
                : "Birden fazla arşiv var, hangisi olduğunu verin:\n  "
                + String.join("\n  ", komsular.stream().map(p -> ReadmeGenerator.repoKokuBul(p).toString()).toList()));
    }

    private static int yeniArsiv(List<String> a) {
        MufredatAyari mufredat = new MufredatAyari();
        List<String> mufredatlar = hepsi(a, "--mufredat");
        if (!mufredatlar.isEmpty()) {
            // Müfredat geçişi olmayan bölüm: tek seçenek, eski müfredat notu kapalı.
            mufredat.secenekler = mufredatlar;
            mufredat.eski_mufredat_oneki = "";
        }
        var bilgi = new ArsivSablonu.BolumBilgisi(tek(a, "--bolum", ""), tek(a, "--fakulte", ""),
                tek(a, "--github", ""), Integer.parseInt(tek(a, "--sinif", "4")), hepsi(a, "--havuz"),
                mufredat, tek(a, "--drive", ""));
        ArsivSablonu.yeniArsivOlustur(Path.of(tek(a, "--yeni-arsiv", "")), bilgi, System.out::println);
        return 0;
    }

    private static String tek(List<String> a, String ad, String varsayilan) {
        int i = a.indexOf(ad);
        if (i < 0) {
            return varsayilan;
        }
        if (i + 1 >= a.size()) {
            throw new IllegalArgumentException(ad + " için değer verilmedi.");
        }
        return a.get(i + 1);
    }

    private static List<String> hepsi(List<String> a, String ad) {
        List<String> sonuc = new ArrayList<>();
        for (int i = 0; i + 1 < a.size(); i++) {
            if (a.get(i).equals(ad)) {
                sonuc.add(a.get(i + 1));
            }
        }
        return sonuc;
    }
}
