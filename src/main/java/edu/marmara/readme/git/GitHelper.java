package edu.marmara.readme.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Python git_helper.py'nin GitHelper sınıfının Java portu — statik git komut sarmalayıcıları. */
public final class GitHelper {

    private GitHelper() {
    }

    public record SonucCikti(int exitCode, String stdout, String stderr) {
        public boolean basarili() {
            return exitCode == 0;
        }
    }

    private static SonucCikti calistir(Path repoPath, String... args) {
        List<String> komut = new ArrayList<>();
        komut.add("git");
        komut.add("-C");
        komut.add(repoPath.toString());
        for (String a : args) {
            komut.add(a);
        }
        try {
            Process p = new ProcessBuilder(komut).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = p.waitFor();
            return new SonucCikti(exit, out, err);
        } catch (IOException | InterruptedException e) {
            return new SonucCikti(-1, "", e.getMessage());
        }
    }

    /**
     * Değişen dosyalar, her biri "XY yol" biçiminde (XY: git durum kodu). -z ile okunur: boşluklu ya da
     * Türkçe karakterli yollar ("1-1/Türkçe 1/README.md") tırnaksız, olduğu gibi gelir ve git add'e
     * doğrudan verilebilir. Yeni klasörlerin içindeki dosyalar tek tek listelenir.
     */
    public static List<String> gitStatus(Path repoPath) {
        SonucCikti r = calistir(repoPath, "status", "--porcelain", "-z", "--untracked-files=all");
        List<String> satirlar = new ArrayList<>();
        String[] parcalar = r.stdout().split("\0");
        for (int i = 0; i < parcalar.length; i++) {
            String p = parcalar[i];
            if (p.length() < 4) {
                continue;
            }
            satirlar.add(p);
            if (p.charAt(0) == 'R' || p.charAt(0) == 'C') {
                i++; // yeniden adlandırmada ardından eski yol gelir
            }
        }
        return satirlar;
    }

    public static SonucCikti gitAdd(Path repoPath, String filePath) {
        return calistir(repoPath, "add", "--", filePath);
    }

    public static SonucCikti gitAddAll(Path repoPath) {
        return calistir(repoPath, "add", "--all");
    }

    public static SonucCikti gitResetAll(Path repoPath) {
        return calistir(repoPath, "reset", "HEAD");
    }

    public static SonucCikti gitReset(Path repoPath, String filePath) {
        return calistir(repoPath, "reset", "--", filePath);
    }

    public static SonucCikti gitCommit(Path repoPath, String message) {
        return calistir(repoPath, "commit", "-m", message);
    }

    public static SonucCikti gitPush(Path repoPath) {
        return calistir(repoPath, "push");
    }

    /**
     * GitHub'daki yeni commit'leri alır, yerel commit'leri üstüne dizer (yerel commit'ler kaybolmaz).
     * Commit'e alınmamış değişiklikler geçici olarak kenara konup geri getirilir.
     */
    public static SonucCikti gitPullRebase(Path repoPath) {
        return calistir(repoPath, "pull", "--rebase", "--autostash");
    }

    /**
     * Çakışmada yerel commit'in satırları geçerli olur (rebase'de "theirs" yeniden uygulanan yerel commit'tir).
     * Çakışmalar botların yazdığı yıldız/yorum bloklarında çıkar; botlar onları her çalışmada yeniden yazar.
     */
    public static SonucCikti gitPullRebaseYerelOncelikli(Path repoPath) {
        return calistir(repoPath, "pull", "--rebase", "--autostash", "-X", "theirs");
    }

    /** GitHub'a henüz gönderilmemiş yerel commit sayısı (izlenen dal yoksa 0). */
    public static int gonderilmemisCommitSayisi(Path repoPath) {
        SonucCikti r = calistir(repoPath, "rev-list", "--count", "@{u}..HEAD");
        try {
            return r.basarili() ? Integer.parseInt(r.stdout().strip()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static SonucCikti gitRebaseAbort(Path repoPath) {
        return calistir(repoPath, "rebase", "--abort");
    }

    public static SonucCikti gitRestore(Path repoPath, String filePath) {
        return calistir(repoPath, "restore", "--", filePath);
    }

    public static SonucCikti gitDiff(Path repoPath, String fileName) {
        return calistir(repoPath, "diff", fileName);
    }

    public static String getFileContentAtCommit(Path repoPath, String filePath, String commitHash) {
        if ("WORKING".equals(commitHash)) {
            try {
                return java.nio.file.Files.readString(repoPath.resolve(filePath), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return "Dosya Silindi";
            }
        }
        SonucCikti r = calistir(repoPath, "show", commitHash + ":" + filePath);
        return r.basarili() ? r.stdout() : "";
    }
}
