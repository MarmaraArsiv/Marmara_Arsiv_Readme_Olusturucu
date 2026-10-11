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

    public static void setGitQuotepathOff(Path repoPath) {
        calistir(repoPath, "config", "core.quotepath", "off");
    }

    /** git status --porcelain çıktısını satır satır döner (boş satırlar dahil, Python ile aynı split davranışı). */
    public static List<String> gitStatus(Path repoPath) {
        setGitQuotepathOff(repoPath);
        SonucCikti r = calistir(repoPath, "status", "--porcelain");
        return List.of(r.stdout().split("\n", -1));
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

    /** GitHub'daki yeni commit'leri alır, yerel commit'leri üstüne dizer (yerel commit'ler kaybolmaz). */
    public static SonucCikti gitPullRebase(Path repoPath) {
        return calistir(repoPath, "pull", "--rebase");
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
