package edu.marmara.readme.engine;

import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.Havuz;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.MufredatAyari;
import edu.marmara.readme.engine.util.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Year;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marmara'nın her bölümü için aynı yapıda arşiv: yeni bölüm arşivini aracın içindeki şablondan kurar ve
 * mevcut arşivlerin GitHub betiklerini, iş akışlarını ve form betiklerini şablonla eşitler.
 *
 * Şablon {@code src/main/resources/sablon} altındadır, hangi dosyanın nereye yazılacağı {@code dosyalar.txt}'te.
 * Betiklerin tek kaynağı burasıdır: iyileştirme aracın reposunda yapılır, her bölüm "Altyapıyı Güncelle" ile alır.
 */
public final class ArsivSablonu {

    private static final String SABLON = "/sablon/";
    private static final Pattern YER_TUTUCU = Pattern.compile("\\{\\{([A-Z_]+)}}");
    private static final Pattern GITHUB_REPO = Pattern.compile("^(?:https?://github\\.com/|git@github\\.com:)([^/]+)/([^/]+?)(?:\\.git)?/*$");

    private ArsivSablonu() {
    }

    /** Yeni bölüm arşivi için sihirbazdan gelen bilgiler. */
    public record BolumBilgisi(String bolumAdi, String fakulteAdi, String githubUrl, int sinifSayisi,
                               List<String> havuzlar, MufredatAyari mufredat, String driveKlasoru) {
    }

    private record SablonDosyasi(boolean altyapi, boolean doldur, String kaynak, String hedef) {
    }

    // ------------------------------------------------------------ yeni arşiv

    /**
     * Hedef klasörde yeni bir bölüm arşivi kurar: şablon dosyaları, JSON'lar, dönem ve havuz klasörleri ve
     * JSON'dan üretilmiş README'ler. Hedef boş olmalı (yalnızca .git ve GitHub'ın açtığı README/LICENSE olabilir).
     *
     * @return arşivin JSON klasörü
     */
    public static Path yeniArsivOlustur(Path hedef, BolumBilgisi b, Consumer<String> log) {
        List<String> hatalar = bilgiHatalari(b);
        if (!hatalar.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", hatalar));
        }
        Path kok = hedef.toAbsolutePath().normalize();
        try {
            if (Files.exists(kok)) {
                try (var icerik = Files.list(kok)) {
                    List<String> dolu = icerik.map(p -> p.getFileName().toString())
                            .filter(ad -> !List.of(".git", "README.md", "LICENSE").contains(ad)).sorted().toList();
                    if (!dolu.isEmpty()) {
                        throw new IllegalArgumentException(kok + " boş değil: " + String.join(", ", dolu));
                    }
                }
            }
            Map<String, String> degerler = yerTutucular(b);
            for (SablonDosyasi d : dosyalar()) {
                String metin = sablonOku(d.kaynak());
                if (d.doldur()) {
                    metin = doldur(metin, degerler, d.hedef().endsWith(".json"));
                }
                yaz(kok.resolve(d.hedef()), metin);
            }
            log.accept("Şablon dosyaları yazıldı.");

            Path json = kok.resolve(".github/json_dosyalari");
            Konfigurasyon k = new Konfigurasyon();
            k.github_url = b.githubUrl().strip();
            k.dokumanlar_repo_yolu = "../..";
            k.mufredat = b.mufredat();
            JsonUtil.write(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), k);
            JsonUtil.write(json.resolve(ReadmeGenerator.DONEMLER_JSON), donemler(b));

            new ReadmeGenerator(json, log).generateAll();
            log.accept("Yeni arşiv hazır: " + kok);
            return json;
        } catch (IOException e) {
            throw new RuntimeException("Arşiv oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /** Sihirbazdaki alanların hataları; boşsa bilgiler geçerli. */
    public static List<String> bilgiHatalari(BolumBilgisi b) {
        List<String> hatalar = new ArrayList<>();
        if (b.bolumAdi().isBlank()) {
            hatalar.add("Bölüm adı boş olamaz.");
        }
        if (b.fakulteAdi().isBlank()) {
            hatalar.add("Fakülte adı boş olamaz.");
        }
        if (!GITHUB_REPO.matcher(b.githubUrl().strip()).matches()) {
            hatalar.add("GitHub adresi https://github.com/<kurum>/<repo> biçiminde olmalı.");
        }
        if (b.sinifSayisi() < 1 || b.sinifSayisi() > 6) {
            hatalar.add("Sınıf sayısı 1 ile 6 arasında olmalı.");
        }
        if (b.havuzlar().stream().anyMatch(h -> h.isBlank() || h.contains("/") || h.matches("\\d+-\\d+"))) {
            hatalar.add("Havuz adları boş olamaz, \"/\" içeremez ve dönem klasörü (ör. 1-1) gibi olamaz.");
        }
        if (b.havuzlar().stream().distinct().count() != b.havuzlar().size()) {
            hatalar.add("Aynı havuz iki kez yazılmış.");
        }
        if (b.mufredat().secenekler.isEmpty()) {
            hatalar.add("En az bir müfredat seçeneği olmalı.");
        }
        return hatalar;
    }

    /** 1-1 … N-2 dönemleri ve seçmeli havuzları. */
    static DonemlerBolumu donemler(BolumBilgisi b) {
        DonemlerBolumu d = new DonemlerBolumu();
        for (int yil = 1; yil <= b.sinifSayisi(); yil++) {
            for (int yari = 1; yari <= 2; yari++) {
                Donem donem = new Donem();
                donem.yil = yil;
                donem.donem = yari == 1 ? "Güz" : "Bahar";
                donem.klasor = yil + "-" + yari;
                donem.donem_adi = yil + ". Sınıf " + donem.donem + " Dönemi";
                d.donemler.add(donem);
            }
        }
        for (String ad : b.havuzlar()) {
            Havuz h = new Havuz();
            h.ad = ad.strip();
            h.klasor = ad.strip();
            d.havuzlar.add(h);
        }
        return d;
    }

    static Map<String, String> yerTutucular(BolumBilgisi b) {
        Map<String, String> m = new LinkedHashMap<>(repoYerTutuculari(b.githubUrl()));
        m.put("BOLUM_ADI", b.bolumAdi().strip());
        m.put("FAKULTE_ADI", b.fakulteAdi().strip());
        m.put("DRIVE_KLASORU", b.driveKlasoru().isBlank() ? b.bolumAdi().strip() : b.driveKlasoru().strip());
        m.put("YIL", String.valueOf(Year.now().getValue()));
        m.put("SAYAC_KIMLIGI", sha1(m.get("REPO").toLowerCase()));
        m.put("HAVUZ_CUMLESI", havuzCumlesi(b.havuzlar()));
        return m;
    }

    /** "Seçmeli dersler `A/`, `B/` ve `C/` altında." — havuz yoksa boş. */
    static String havuzCumlesi(List<String> havuzlar) {
        List<String> adlar = havuzlar.stream().map(h -> "`" + h.strip() + "/`").toList();
        if (adlar.isEmpty()) {
            return "";
        }
        String liste = adlar.size() == 1 ? adlar.get(0)
                : String.join(", ", adlar.subList(0, adlar.size() - 1)) + " ve " + adlar.get(adlar.size() - 1);
        return "Seçmeli dersler " + liste + " altında.";
    }

    // ------------------------------------------------------------ altyapı güncelleme

    /**
     * Arşivin altyapı dosyalarını (GitHub betikleri, iş akışları, form betikleri) aracın şablonuyla karşılaştırır.
     * yaz true ise farklı olanların üzerine yazar.
     *
     * @return şablondan farklı olan (yaz true ise güncellenen) dosyalar, repo köküne göre
     */
    public static List<Path> altyapiyiGuncelle(Path repoKoku, boolean yaz) {
        Path json = ReadmeGenerator.jsonKlasoruBul(repoKoku);
        Konfigurasyon k = json == null ? null : JsonUtil.read(json.resolve(ReadmeGenerator.KONFIGURASYON_JSON), Konfigurasyon.class);
        if (k == null) {
            throw new IllegalArgumentException(repoKoku + " bir arşiv değil (.github/json_dosyalari/konfigurasyon.json yok).");
        }
        if (!GITHUB_REPO.matcher(k.github_url.strip()).matches()) {
            throw new IllegalArgumentException("konfigurasyon.json'daki github_url bir GitHub repo adresi değil: " + k.github_url);
        }
        Map<String, String> degerler = repoYerTutuculari(k.github_url);
        List<Path> farkli = new ArrayList<>();
        try {
            for (SablonDosyasi d : dosyalar()) {
                if (!d.altyapi()) {
                    continue;
                }
                String metin = sablonOku(d.kaynak());
                if (d.doldur()) {
                    metin = doldur(metin, degerler, false);
                }
                Path yol = repoKoku.resolve(d.hedef());
                if (!Files.exists(yol) || !Files.readString(yol, StandardCharsets.UTF_8).equals(metin)) {
                    farkli.add(Path.of(d.hedef()));
                    if (yaz) {
                        yaz(yol, metin);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Altyapı dosyaları güncellenemedi: " + e.getMessage(), e);
        }
        return farkli;
    }

    // ------------------------------------------------------------ yardımcılar

    private static Map<String, String> repoYerTutuculari(String githubUrl) {
        Matcher m = GITHUB_REPO.matcher(githubUrl.strip());
        if (!m.matches()) {
            throw new IllegalArgumentException("GitHub adresi anlaşılamadı: " + githubUrl);
        }
        Map<String, String> d = new LinkedHashMap<>();
        d.put("REPO", m.group(1) + "/" + m.group(2));
        d.put("REPO_ADI", m.group(2));
        return d;
    }

    private static List<SablonDosyasi> dosyalar() {
        List<SablonDosyasi> liste = new ArrayList<>();
        for (String satir : sablonOku("dosyalar.txt").split("\n")) {
            String s = satir.strip();
            if (s.isEmpty() || s.startsWith("#")) {
                continue;
            }
            String[] p = s.split("\\s+");
            if (p.length != 3 || !p[0].matches("(altyapi|kurulum)\\+?")) {
                throw new IllegalStateException("dosyalar.txt satırı anlaşılamadı: " + satir);
            }
            liste.add(new SablonDosyasi(p[0].startsWith("altyapi"), p[0].endsWith("+"), p[1], p[2]));
        }
        return liste;
    }

    private static String sablonOku(String yol) {
        try (InputStream in = ArsivSablonu.class.getResourceAsStream(SABLON + yol)) {
            if (in == null) {
                throw new IllegalStateException("Şablon dosyası bulunamadı: " + yol);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** {{AD}} yer tutucularını doldurur; JSON dosyalarında değerler JSON metni içine kaçışlanır. */
    static String doldur(String metin, Map<String, String> degerler, boolean json) {
        Matcher m = YER_TUTUCU.matcher(metin);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String deger = degerler.get(m.group(1));
            if (deger == null) {
                throw new IllegalStateException("Şablonda tanımsız yer tutucu: " + m.group());
            }
            if (json) {
                deger = deger.replace("\\", "\\\\").replace("\"", "\\\"");
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(deger));
        }
        return m.appendTail(sb).toString();
    }

    private static void yaz(Path yol, String metin) throws IOException {
        Files.createDirectories(yol.getParent());
        Files.writeString(yol, metin, StandardCharsets.UTF_8);
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
