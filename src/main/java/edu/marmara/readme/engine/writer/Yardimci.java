package edu.marmara.readme.engine.writer;

import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.Hoca;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Python writers/yardimci.py'nin Java portu — tüm writer'ların ortak yardımcı fonksiyonları. */
public final class Yardimci {

    private Yardimci() {
    }

    public record DetayEtiketleri(String acilis, String kapanis) {
    }

    /** Puanı (0-100 arası) yıldız gösterimine çevirir (örn "★★★★★☆☆☆☆☆"). Round-half-to-even. */
    public static String puanlariYildizaCevir(double puan, int maxYildizSayisi) {
        double yuvarlanmisPuan = Math.rint(puan / 10.0) * 10;
        int doluYildizSayisi = (int) Math.floor(yuvarlanmisPuan / 10.0);
        int bosYildizSayisi = maxYildizSayisi - doluYildizSayisi;
        return "★".repeat(Math.max(0, doluYildizSayisi)) + "☆".repeat(Math.max(0, bosYildizSayisi));
    }

    public static String puanlariYildizaCevir(double puan) {
        return puanlariYildizaCevir(puan, 10);
    }

    private static final Pattern BASLIK_LINKI_TEMIZLE =
            Pattern.compile("[^\\w\\s-]", Pattern.UNICODE_CHARACTER_CLASS);

    /** Markdown başlık linkini oluşturur (örn "(#-baslik-adi)"). */
    public static String baslikLinkiOlustur(String baslik) {
        String temiz = BASLIK_LINKI_TEMIZLE.matcher(baslik).replaceAll("");
        temiz = temiz.replace(" ", "-").toLowerCase(Locale.ROOT);
        return "(#-" + temiz + ")";
    }


    /** HTML details etiketlerinin açılış/kapanış çiftini oluşturur. */
    public static DetayEtiketleri detayEtiketleriOlustur(String baslik, String girinti) {
        String acilis = girinti + "<details>\n" + girinti + "<summary><b>" + baslik + "</b></summary>\n\n";
        String kapanis = girinti + "</details>\n";
        return new DetayEtiketleri(acilis, kapanis);
    }

    /** Hoca sıralaması: aktif görevde olanlar önce, sonra ünvan önceliği (Prof>Doç>Dr>diğer), sonra ad. */
    public static final Comparator<Hoca> HOCA_SIRALAMA = Comparator
            .<Hoca>comparingInt(h -> h.hoca_aktif_gorevde_mi ? 0 : 1)
            .thenComparingInt(h -> {
                String ad = h.ad == null ? "" : h.ad;
                String unvan = ad.isEmpty() ? "" : ad.split("\\s+")[0];
                return Sabitler.UNVAN_ONCELIKLERI.getOrDefault(unvan, 4);
            })
            .thenComparing(h -> h.ad == null ? "" : h.ad);

    /** Ders sıralaması: ad üzerinden, Türkçe İ->i çevrilip küçültülerek. */
    public static final Comparator<Ders> DERS_SIRALAMA =
            Comparator.comparing(d -> dersSiralamaAnahtari(d.ad));

    public static String dersSiralamaAnahtari(String ad) {
        String a = (ad == null || ad.isEmpty()) ? "Z" : ad;
        return a.replace("İ", "i").toLowerCase(Locale.ROOT);
    }

    /** Dönem grup başlığı sıralama anahtarı (örn "2. Yıl - Güz", "Lisansüstü", ...). */
    public static int[] donemSiralamasi(String donemKey) {
        if (Sabitler.LISANSUSTU.equals(donemKey)) {
            return new int[]{1499, 1499};
        }
        if (Sabitler.ARTIK_MUFREDATA_DAHIL_OLMAYAN_DERSLER.equals(donemKey)) {
            return new int[]{1500, 1500};
        }
        if (Sabitler.MESLEKI_SECMELI.equals(donemKey)) {
            return new int[]{998, 998};
        }
        try {
            String[] parts = donemKey.split(" - ");
            int yil = Integer.parseInt(parts[0].split("\\.")[0].trim());
            int donemIdx = "Güz".equals(parts[1].trim()) ? 0 : 1;
            return new int[]{yil, donemIdx};
        } catch (Exception e) {
            return new int[]{999, 999};
        }
    }

    public static final Comparator<String> DONEM_GRUP_SIRALAMA = (a, b) -> {
        int[] ka = donemSiralamasi(a);
        int[] kb = donemSiralamasi(b);
        if (ka[0] != kb[0]) {
            return Integer.compare(ka[0], kb[0]);
        }
        return Integer.compare(ka[1], kb[1]);
    };

    /** Yerel klasör yolunu repo köküne göreceli, URL-encode edilmiş bir GitHub linkine çevirir. */
    public static String yerelYoldanGithubLinkine(Path klasorYolu, Path dokumanlarRepoYolu) {
        if (klasorYolu == null) {
            return null;
        }
        Path rel = dokumanlarRepoYolu.toAbsolutePath().normalize()
                .relativize(klasorYolu.toAbsolutePath().normalize());
        String yol = rel.toString().replace("\\", "/");
        yol = stripLeadingSlashes(yol);
        String encoded = Stream.of(yol.split("/"))
                .map(p -> URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(Collectors.joining("/"));
        return "./" + encoded;
    }

    private static String stripLeadingSlashes(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == '/') {
            i++;
        }
        return s.substring(i);
    }

    /** Elemanı, anahtar fonksiyonuna göre sıralı listeye doğru konuma ekler (bisect benzeri, O(n)). */
    public static <T> void siraliEkle(List<T> liste, T eleman, java.util.function.Function<T, String> anahtar) {
        String elemanAnahtar = anahtar.apply(eleman);
        int konum = 0;
        while (konum < liste.size() && anahtar.apply(liste.get(konum)).compareTo(elemanAnahtar) < 0) {
            konum++;
        }
        liste.add(konum, eleman);
    }

    // --- Link göreceli hale getirme pipeline'ı ------------------------------------------------

    private static String karsilastirmaNormu(String metin) {
        String normalized = Normalizer.normalize(metin.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        return normalized.replaceAll("\\p{M}", "");
    }

    private static String repoGoreceliYol(Path klasorYolu, Path dokumanlarRepoYolu) {
        if (klasorYolu == null) {
            return "";
        }
        Path rel = dokumanlarRepoYolu.toAbsolutePath().normalize()
                .relativize(klasorYolu.toAbsolutePath().normalize());
        String yol = rel.toString().replace("\\", "/");
        return stripLeadingSlashes(yol);
    }

    private static List<String> repoUstDizinleri(Path dokumanlarRepoYolu) {
        List<String> sonuc = new ArrayList<>();
        try (Stream<Path> list = Files.list(dokumanlarRepoYolu)) {
            list.filter(Files::isDirectory)
                    .forEach(p -> sonuc.add(karsilastirmaNormu(p.getFileName().toString())));
        } catch (IOException ignored) {
            // Python tarafı da OSError'ı yutup boş küme döndürüyor.
        }
        return sonuc;
    }

    private static String goreceliYol(String hedef, String baseRel) {
        List<String> baseParcalar = splitNonEmpty(baseRel);
        List<String> hedefParcalar = splitNonEmpty(hedef);
        int ortak = 0;
        while (ortak < baseParcalar.size() && ortak < hedefParcalar.size()
                && karsilastirmaNormu(baseParcalar.get(ortak)).equals(karsilastirmaNormu(hedefParcalar.get(ortak)))) {
            ortak++;
        }
        List<String> parcalar = new ArrayList<>();
        for (int i = 0; i < baseParcalar.size() - ortak; i++) {
            parcalar.add("..");
        }
        parcalar.addAll(hedefParcalar.subList(ortak, hedefParcalar.size()));
        if (parcalar.isEmpty()) {
            return "./";
        }
        if (parcalar.get(0).equals("..")) {
            return String.join("/", parcalar);
        }
        return "./" + String.join("/", parcalar);
    }

    private static List<String> splitNonEmpty(String s) {
        List<String> out = new ArrayList<>();
        for (String p : s.split("/")) {
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    /** Göreceli yolun bileşenlerini gerçek diskteki klasör/dosya adlarıyla düzeltir (harf büyüklüğü farkına toleranslı). */
    private static String diskYolDuzelt(Path baseKlasorYolu, String goreceli) {
        Path suanki = baseKlasorYolu;
        List<String> sonuc = new ArrayList<>();
        boolean bozuk = false;
        for (String parca : goreceli.split("/")) {
            if (parca.equals("..") || parca.equals(".")) {
                sonuc.add(parca);
                if (parca.equals("..") && suanki != null) {
                    suanki = suanki.getParent();
                }
                continue;
            }
            if (!bozuk && suanki != null) {
                String eslesen = null;
                try (Stream<Path> list = Files.list(suanki)) {
                    eslesen = list.map(p -> p.getFileName().toString())
                            .filter(ad -> karsilastirmaNormu(ad).equals(karsilastirmaNormu(parca)))
                            .findFirst().orElse(null);
                } catch (IOException ignored) {
                }
                if (eslesen != null) {
                    sonuc.add(eslesen);
                    suanki = suanki.resolve(eslesen);
                    continue;
                }
                bozuk = true;
            }
            sonuc.add(parca);
        }
        return String.join("/", sonuc);
    }

    private static final Pattern MARKDOWN_LINK = Pattern.compile("]\\(([^)]+)\\)");

    /**
     * Metindeki markdown linklerini, README'nin bulunduğu klasöre göreceli hale getirir.
     * ders_klasoru_yolu null ise base ile aynı kabul edilir (ders README'si durumu).
     */
    public static String kaynakLinkleriniGoreceliYap(String metin, Path baseKlasorYolu, Path dersKlasorYolu,
                                                       Path dokumanlarRepoYolu) {
        if (baseKlasorYolu == null) {
            return metin;
        }
        String baseRel = repoGoreceliYol(baseKlasorYolu, dokumanlarRepoYolu);
        if (baseRel.isEmpty()) {
            return metin;
        }
        String dersRel = dersKlasorYolu != null ? repoGoreceliYol(dersKlasorYolu, dokumanlarRepoYolu) : baseRel;
        List<String> ustDizinler = repoUstDizinleri(dokumanlarRepoYolu);

        Matcher m = MARKDOWN_LINK.matcher(metin);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(metin, last, m.start());
            String url = m.group(1);
            if (!url.startsWith("./")) {
                sb.append(m.group(0));
            } else {
                String hedefRaw = java.net.URLDecoder.decode(url.substring(2), StandardCharsets.UTF_8);
                String sonEgik = hedefRaw.endsWith("/") ? "/" : "";
                String hedef = hedefRaw.endsWith("/") ? hedefRaw.substring(0, hedefRaw.length() - 1) : hedefRaw;
                String ilk = hedef.isEmpty() ? "" : hedef.split("/")[0];
                String repoRel;
                if (!ilk.isEmpty() && ustDizinler.contains(karsilastirmaNormu(ilk))) {
                    repoRel = hedef;
                } else if (dersRel.equals(baseRel)) {
                    sb.append(m.group(0));
                    last = m.end();
                    continue;
                } else {
                    repoRel = hedef.isEmpty() ? dersRel : dersRel + "/" + hedef;
                }
                String goreceli = goreceliYol(repoRel, baseRel);
                goreceli = diskYolDuzelt(baseKlasorYolu, goreceli);
                String yeni = Stream.of(goreceli.split("/"))
                        .map(p -> URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"))
                        .collect(Collectors.joining("/"));
                sb.append("](").append(yeni).append(sonEgik).append(")");
            }
            last = m.end();
        }
        sb.append(metin, last, metin.length());
        return sb.toString();
    }

}
