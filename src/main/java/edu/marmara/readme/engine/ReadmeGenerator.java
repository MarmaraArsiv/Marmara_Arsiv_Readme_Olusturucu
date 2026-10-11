package edu.marmara.readme.engine;

import edu.marmara.readme.engine.config.KonfigurasyonJsonKontrol;
import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.model.Havuz;
import edu.marmara.readme.engine.model.Hoca;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.model.KatkidaBulunanlarBolumu;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.model.RepoKullanimiBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.writer.AnaReadmeWriter;
import edu.marmara.readme.engine.writer.DersKlasorWriter;
import edu.marmara.readme.engine.writer.DonemWriter;
import edu.marmara.readme.engine.writer.KorunanBloklar;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * JSON dosyalarından arşivin README'lerini üretir: kök README, 8 dönem README'si, seçmeli havuz
 * README'leri ve her dersin kendi README'si.
 *
 * GitHub'ın doldurduğu bloklar (yıldızlar, onaylı yorumlar, doluluk tablosu) mevcut README'lerden
 * okunup aynen korunur. Hiçbir veri değişmediyse çıktı mevcut README'lerle birebir aynıdır.
 */
public class ReadmeGenerator {

    public static final String GIRIS_JSON = "giris.json";
    public static final String REPO_KULLANIMI_JSON = "repo_kullanimi.json";
    public static final String DERSLER_JSON = "dersler.json";
    public static final String HOCALAR_JSON = "hocalar.json";
    public static final String KATKIDA_BULUNANLAR_JSON = "katkida_bulunanlar.json";
    public static final String DONEMLER_JSON = "donemler.json";
    public static final String KONFIGURASYON_JSON = "konfigurasyon.json";

    private final Path jsonDeposu;
    private final Consumer<String> ilerlemeDinleyicisi;

    private Path repoKoku;
    private DerslerBolumu dersler;
    private HocalarBolumu hocalar;
    private boolean hocaDersleriDegisti;

    /** Arşivde JSON klasörünün aranacağı yerler (yeni yer önce). */
    private static final List<String> JSON_KLASORU_ADAYLARI = List.of(".github/json_dosyalari", "json_dosyalari");

    /**
     * Verilen yol JSON klasörünün kendisi, arşiv reposunun kökü ya da JSON klasörünün eski yeri
     * (json_dosyalari, .github/ altına taşınmadan önceki) olabilir; JSON klasörünü döner, bulamazsa null.
     */
    public static Path jsonKlasoruBul(Path verilen) {
        if (verilen == null) {
            return null;
        }
        Path yol = verilen.toAbsolutePath().normalize();
        if (Files.exists(yol.resolve(KONFIGURASYON_JSON))) {
            return yol;
        }
        for (Path kok : new Path[]{yol, yol.getParent()}) {
            if (kok == null) {
                continue;
            }
            for (String aday : JSON_KLASORU_ADAYLARI) {
                if (Files.exists(kok.resolve(aday).resolve(KONFIGURASYON_JSON))) {
                    return kok.resolve(aday);
                }
            }
        }
        return null;
    }

    /** Verilen klasörün içindeki arşiv repolarının JSON klasörleri (aracın yanındaki bölüm arşivleri). */
    public static List<Path> komsuArsivler(Path ust) {
        List<Path> sonuc = new ArrayList<>();
        try (Stream<Path> s = Files.list(ust)) {
            for (Path p : s.filter(Files::isDirectory).sorted().toList()) {
                Path json = p.resolve(JSON_KLASORU_ADAYLARI.get(0));
                if (Files.exists(json.resolve(KONFIGURASYON_JSON))) {
                    sonuc.add(json.toAbsolutePath().normalize());
                }
            }
        } catch (IOException ignored) {
            // okunamayan klasör: arşiv yok sayılır
        }
        return sonuc;
    }

    public ReadmeGenerator(Path jsonDeposu, Consumer<String> ilerlemeDinleyicisi) {
        this.jsonDeposu = jsonDeposu;
        this.ilerlemeDinleyicisi = ilerlemeDinleyicisi != null ? ilerlemeDinleyicisi : s -> {
        };
    }

    private void log(String mesaj) {
        ilerlemeDinleyicisi.accept(mesaj);
    }

    private <T> T oku(String dosya, Class<T> tip) {
        T veri = JsonUtil.read(jsonDeposu.resolve(dosya), tip);
        if (veri == null) {
            throw new IllegalStateException(jsonDeposu.resolve(dosya) + " bulunamadı.");
        }
        return veri;
    }

    /** konfigurasyon.json'daki dokumanlar_repo_yolu'na göre arşiv reposunun kökü (dosyaya bir şey yazmaz). */
    public static Path repoKokuBul(Path jsonDeposu) {
        Konfigurasyon k = JsonUtil.read(jsonDeposu.resolve(KONFIGURASYON_JSON), Konfigurasyon.class);
        return jsonDeposu.resolve(k != null ? k.dokumanlar_repo_yolu : new Konfigurasyon().dokumanlar_repo_yolu)
                .toAbsolutePath().normalize();
    }

    /** Tüm README'lerin olması gereken içerikleri: repo köküne göre yol → içerik. Diske bir şey yazmaz. */
    public Map<Path, String> uret() {
        log("Veriler yükleniyor...");
        Konfigurasyon konfig = KonfigurasyonJsonKontrol.guncelle(jsonDeposu.resolve(KONFIGURASYON_JSON), jsonDeposu);
        repoKoku = jsonDeposu.resolve(konfig.dokumanlar_repo_yolu).normalize();
        GirisBolumu giris = oku(GIRIS_JSON, GirisBolumu.class);
        RepoKullanimiBolumu repoKullanimi = oku(REPO_KULLANIMI_JSON, RepoKullanimiBolumu.class);
        dersler = oku(DERSLER_JSON, DerslerBolumu.class);
        hocalar = oku(HOCALAR_JSON, HocalarBolumu.class);
        DonemlerBolumu donemler = oku(DONEMLER_JSON, DonemlerBolumu.class);
        KatkidaBulunanlarBolumu katkida = oku(KATKIDA_BULUNANLAR_JSON, KatkidaBulunanlarBolumu.class);

        dogrula(donemler, konfig);
        Map<String, Hoca> hocaAdina = new HashMap<>();
        hocalar.hocalar.forEach(h -> hocaAdina.put(h.ad, h));
        hocaDersleriDegisti = hocaDersleriniEsitle();

        log("Mevcut README'lerdeki GitHub blokları okunuyor...");
        List<String> donemKlasorleri = donemler.donemler.stream().map(d -> d.klasor).toList();
        List<String> dersKlasorleri = dersler.dersler.stream().map(d -> d.klasor).toList();
        KorunanBloklar korunan = KorunanBloklar.oku(repoKoku, donemKlasorleri, dersKlasorleri);

        Map<Path, String> sonuc = new LinkedHashMap<>();
        log("Ana README hazırlanıyor...");
        sonuc.put(Path.of(Sabitler.README_MD), new AnaReadmeWriter(giris, repoKullanimi, dersler, hocalar,
                donemler, katkida, konfig, korunan, hocaAdina).yaz());

        DonemWriter donemWriter = new DonemWriter(hocaAdina, korunan, konfig);
        for (Donem donem : donemler.donemler) {
            log(donem.donem_adi + " hazırlanıyor...");
            sonuc.put(Path.of(donem.klasor, Sabitler.README_MD), donemWriter.yaz(donem, derslerIn(donem.klasor)));
        }
        for (Havuz havuz : donemler.havuzlar) {
            log(havuz.ad + " hazırlanıyor...");
            sonuc.put(Path.of(havuz.klasor, Sabitler.README_MD), donemWriter.yazHavuz(havuz, derslerIn(havuz.klasor)));
        }
        DersKlasorWriter dersWriter = new DersKlasorWriter(hocaAdina, korunan, konfig, dersler.katkida_bulun_metni);
        for (Ders d : dersler.dersler) {
            sonuc.put(Path.of(d.klasor, Sabitler.README_MD), dersWriter.yaz(d));
        }
        return sonuc;
    }

    /**
     * README yazmadan önce JSON'daki tutarsızlıkları yakalar (ör. klasörü boş bir ders ana README'nin
     * üzerine yazılmasın). Hata varsa hiçbir dosyaya dokunmadan hepsini birlikte bildirir.
     */
    private void dogrula(DonemlerBolumu donemler, Konfigurasyon konfig) {
        List<String> hatalar = new ArrayList<>();
        Set<String> ustKlasorler = new HashSet<>();
        for (Donem d : donemler.donemler) {
            klasorKontrol("Dönem \"" + d.donem_adi + "\"", d.klasor, ustKlasorler, hatalar);
        }
        for (Havuz h : donemler.havuzlar) {
            klasorKontrol("Havuz \"" + h.ad + "\"", h.klasor, ustKlasorler, hatalar);
        }
        Set<String> dersKlasorleri = new HashSet<>();
        for (Ders d : dersler.dersler) {
            String ad = "Ders \"" + d.ad + "\"";
            if (d.ad.isBlank()) {
                hatalar.add("Adı boş bir ders var (klasör: " + d.klasor + ").");
            }
            if (!ustKlasorler.contains(d.ustKlasor()) || d.klasorAdi().isBlank() || d.klasor.equals(d.ustKlasor())) {
                hatalar.add(ad + ": klasör \"" + d.klasor + "\" bir dönem/havuz klasörünün altında olmalı (ör. 1-1/Ders Adı).");
            }
            if (!dersKlasorleri.add(d.klasor)) {
                hatalar.add(ad + ": \"" + d.klasor + "\" klasörü birden fazla derste kullanılmış.");
            }
            Set<String> yillar = new HashSet<>();
            for (AkademisyenYili y : d.akademisyenler) {
                if (!y.yil.matches("\\d{4}-\\d{4}")) {
                    hatalar.add(ad + ": akademik yıl \"" + y.yil + "\" 2025-2026 biçiminde olmalı.");
                }
                if (!yillar.add(y.yil)) {
                    hatalar.add(ad + ": " + y.yil + " yılı iki kez girilmiş.");
                }
                if (y.hocalar.isEmpty() || y.hocalar.stream().anyMatch(h -> h.ad.isBlank())) {
                    hatalar.add(ad + ": " + y.yil + " yılında boş hoca adı var.");
                }
            }
        }
        Set<String> gruplar = new HashSet<>();
        hocalar.gruplar.forEach(g -> gruplar.add(g.ad));
        Set<String> adlar = new HashSet<>();
        Set<String> kisaltmalar = new HashSet<>();
        for (Hoca h : hocalar.hocalar) {
            if (h.ad.isBlank() || !adlar.add(h.ad)) {
                hatalar.add("Hoca adı boş ya da iki kez girilmiş: \"" + h.ad + "\".");
            }
            if (h.kisaltma.isBlank() || !kisaltmalar.add(h.kisaltma)) {
                hatalar.add("Hoca \"" + h.ad + "\": kısaltma boş ya da başka bir hocada da var (\"" + h.kisaltma + "\").");
            }
            if (!gruplar.contains(h.grup)) {
                hatalar.add("Hoca \"" + h.ad + "\": grup \"" + h.grup + "\" hocalar.json gruplarında yok.");
            }
        }
        // Formlar henüz kurulmamış olabilir (yeni açılan bölüm); kurulmuşsa soru kimlikleri eksiksiz olmalı.
        Map<String, List<String>> formAlanlari = Map.of(
                "hoca_yorumlama", List.of("hoca_alani", "isim_alani", "yorum_alani"),
                "ders_yorumlama", List.of("ders_alani", "isim_alani", "yorum_alani"),
                "ders_oylama", List.of("ders_alani", "kolaylik_alani", "gereklilik_alani"));
        formAlanlari.forEach((form, alanlar) -> {
            if (!konfig.formAlani(form, "link").isBlank()) {
                for (String alan : alanlar) {
                    if (konfig.formAlani(form, alan).isBlank()) {
                        hatalar.add("konfigurasyon.json: formlar." + form + "." + alan + " boş (formun linki girilmiş).");
                    }
                }
            }
        });
        if (konfig.mufredat.secenekler.isEmpty()) {
            hatalar.add("konfigurasyon.json: mufredat.secenekler boş.");
        }
        if (!hatalar.isEmpty()) {
            throw new IllegalStateException("JSON dosyalarında düzeltilmesi gerekenler var, README'lere dokunulmadı:\n- "
                    + String.join("\n- ", hatalar));
        }
    }

    private static void klasorKontrol(String sahip, String klasor, Set<String> kullanilan, List<String> hatalar) {
        if (klasor == null || klasor.isBlank() || klasor.contains("/")) {
            hatalar.add(sahip + ": klasör boş olamaz ve \"/\" içeremez.");
        } else if (!kullanilan.add(klasor)) {
            hatalar.add(sahip + ": \"" + klasor + "\" klasörü birden fazla dönem/havuzda kullanılmış.");
        }
    }

    private List<Ders> derslerIn(String ustKlasor) {
        return dersler.dersler.stream().filter(d -> d.ustKlasor().equals(ustKlasor)).toList();
    }

    /**
     * Her hocanın "dersler" listesini derslerdeki akademisyen bilgisiyle eşitler: verilmeyen dersler
     * çıkar, yeni dersler sona eklenir (hoca kartında aynı yıl biten derslerin sırası bu listeye göredir).
     */
    private boolean hocaDersleriniEsitle() {
        Map<String, Set<String>> verdigi = new HashMap<>();
        for (Ders d : dersler.dersler) {
            for (AkademisyenYili y : d.akademisyenler) {
                for (DersHocasi h : y.hocalar) {
                    verdigi.computeIfAbsent(h.ad, a -> new LinkedHashSet<>()).add(d.ad);
                }
            }
        }
        boolean degisti = false;
        for (Hoca h : hocalar.hocalar) {
            Set<String> dersAdlari = verdigi.getOrDefault(h.ad, Set.of());
            List<String> yeni = new ArrayList<>();
            for (String ad : h.dersler) {
                if (dersAdlari.contains(ad) && !yeni.contains(ad)) {
                    yeni.add(ad);
                }
            }
            for (String ad : dersAdlari) {
                if (!yeni.contains(ad)) {
                    yeni.add(ad);
                }
            }
            if (!yeni.equals(h.dersler)) {
                h.dersler = yeni;
                degisti = true;
            }
        }
        return degisti;
    }

    /** README'leri diske yazar (yalnızca değişenleri); değişen dosya sayısını döner. */
    public int generateAll() {
        long bas = System.currentTimeMillis();
        Map<Path, String> icerikler = uret();
        int degisen = 0;
        for (Map.Entry<Path, String> e : icerikler.entrySet()) {
            Path yol = repoKoku.resolve(e.getKey());
            if (!e.getValue().equals(mevcut(yol))) {
                try {
                    Files.createDirectories(yol.getParent());
                    Files.writeString(yol, e.getValue(), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    throw new RuntimeException("README yazılamadı: " + yol, ex);
                }
                degisen++;
                log("Güncellendi: " + e.getKey());
            }
        }
        if (hocaDersleriDegisti) {
            JsonUtil.write(jsonDeposu.resolve(HOCALAR_JSON), hocalar);
            log("hocalar.json'daki ders listeleri güncellendi.");
        }
        log(String.format(Locale.ROOT, "%d README güncellendi (%d dosya kontrol edildi, %.1f sn).",
                degisen, icerikler.size(), (System.currentTimeMillis() - bas) / 1000.0));
        return degisen;
    }

    /** Diske yazmadan, üretilen içeriği mevcut dosyadan farklı olan README'leri döner. */
    public List<Path> farkliOlanlar() {
        Map<Path, String> icerikler = uret();
        List<Path> farkli = new ArrayList<>();
        for (Map.Entry<Path, String> e : icerikler.entrySet()) {
            if (!e.getValue().equals(mevcut(repoKoku.resolve(e.getKey())))) {
                farkli.add(e.getKey());
            }
        }
        return farkli;
    }

    public Path repoKoku() {
        return repoKoku;
    }

    private static String mevcut(Path yol) {
        try {
            return Files.exists(yol) ? Files.readString(yol, StandardCharsets.UTF_8) : null;
        } catch (IOException e) {
            throw new RuntimeException("README okunamadı: " + yol, e);
        }
    }
}
