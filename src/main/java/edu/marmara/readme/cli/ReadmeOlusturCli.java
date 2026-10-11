package edu.marmara.readme.cli;

import edu.marmara.readme.engine.ReadmeGenerator;

import java.nio.file.Path;
import java.util.List;

/**
 * Arayüzsüz çalıştırma.
 * <pre>
 *   ReadmeOlusturCli &lt;json-deposu&gt;            README'leri JSON'dan üretip değişenleri yazar
 *   ReadmeOlusturCli &lt;json-deposu&gt; --kontrol  hiçbir şey yazmaz; JSON ile README'ler uyuşmuyorsa
 *                                               farklı dosyaları listeler ve 1 koduyla çıkar
 * </pre>
 */
public final class ReadmeOlusturCli {

    private ReadmeOlusturCli() {
    }

    public static void main(String[] args) {
        Path jsonDeposu = args.length > 0 ? Path.of(args[0]) : Path.of(".");
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
