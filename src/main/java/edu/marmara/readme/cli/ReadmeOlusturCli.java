package edu.marmara.readme.cli;

import edu.marmara.readme.engine.ReadmeGenerator;

import java.nio.file.Path;
import java.util.List;

/**
 * Arayüzsüz çalıştırma. Yol olarak arşiv reposunun kökü ya da JSON klasörü verilebilir.
 * <pre>
 *   ReadmeOlusturCli &lt;arşiv&gt;            README'leri JSON'dan üretip değişenleri yazar
 *   ReadmeOlusturCli &lt;arşiv&gt; --kontrol  hiçbir şey yazmaz; JSON ile README'ler uyuşmuyorsa
 *                                         farklı dosyaları listeler ve 1 koduyla çıkar
 * </pre>
 */
public final class ReadmeOlusturCli {

    private ReadmeOlusturCli() {
    }

    public static void main(String[] args) {
        try {
            calistir(args);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
        }
    }

    private static void calistir(String[] args) {
        Path verilen = args.length > 0 && !args[0].startsWith("--") ? Path.of(args[0]) : Path.of(".");
        Path jsonDeposu = ReadmeGenerator.jsonKlasoruBul(verilen);
        if (jsonDeposu == null) {
            throw new IllegalStateException(verilen.toAbsolutePath() + " altında JSON klasörü bulunamadı "
                    + "(arşiv reposunun kökünü ya da .github/json_dosyalari klasörünü verin).");
        }
        boolean kontrol = List.of(args).contains("--kontrol");
        if (!kontrol) {
            new ReadmeGenerator(jsonDeposu, System.out::println).generateAll();
            return;
        }
        List<Path> farkli = new ReadmeGenerator(jsonDeposu, s -> {
        }).farkliOlanlar();
        if (farkli.isEmpty()) {
            System.out.println("README'ler JSON ile uyumlu.");
            return;
        }
        System.out.println(farkli.size() + " README JSON'dan üretilenle aynı değil:");
        farkli.forEach(p -> System.out.println("  " + p));
        System.exit(1);
    }
}
