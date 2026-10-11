package edu.marmara.readme.gui;

import com.fasterxml.jackson.databind.JsonNode;
import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.config.KonfigurasyonJsonKontrol;
import edu.marmara.readme.engine.model.Konfigurasyon;
import edu.marmara.readme.engine.util.JsonUtil;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bölüm arşivinin ayarları: GitHub adresi, Google Formları (link, soru kimlikleri, cevap CSV'leri) ve müfredat.
 * Form ayarları, form_olustur.gs'in formAyarlari() çıktısı yapıştırılarak tek seferde doldurulabilir.
 */
public class KonfigurasyonWindow {

    /** Form → (alan, etiket); sıra formdaki soru sırasıdır. */
    static final Map<String, Map<String, String>> FORMLAR = new LinkedHashMap<>();

    static {
        FORMLAR.put("hoca_yorumlama", alanlar("hoca_alani", "Hoca sorusu kimliği", "isim_alani", "İsim sorusu kimliği",
                "yorum_alani", "Yorum sorusu kimliği", "hoca_yorumlama_csv", "Cevaplar (CSV) linki"));
        FORMLAR.put("ders_yorumlama", alanlar("ders_alani", "Ders sorusu kimliği", "isim_alani", "İsim sorusu kimliği",
                "yorum_alani", "Yorum sorusu kimliği", "ders_yorumlama_csv", "Cevaplar (CSV) linki"));
        FORMLAR.put("ders_oylama", alanlar("ders_alani", "Ders sorusu kimliği", "kolaylik_alani", "Kolaylık sorusu kimliği",
                "gereklilik_alani", "Gereklilik sorusu kimliği", "ders_oylama_csv", "Cevaplar (CSV) linki"));
    }

    private static final Map<String, String> FORM_ADLARI = Map.of(
            "hoca_yorumlama", "Hoca Yorumlama Formu", "ders_yorumlama", "Ders Yorumlama Formu",
            "ders_oylama", "Ders Oylama Formu");

    private final Path jsonDeposu;

    public KonfigurasyonWindow(Path jsonDeposu) {
        this.jsonDeposu = jsonDeposu;
    }

    public void show() {
        Path konfYolu = jsonDeposu.resolve(ReadmeGenerator.KONFIGURASYON_JSON);
        Konfigurasyon k = KonfigurasyonJsonKontrol.guncelle(konfYolu, jsonDeposu);

        Stage stage = new Stage();
        stage.setTitle("Konfigürasyon Düzenle");
        stage.initModality(Modality.APPLICATION_MODAL);

        TextField githubUrl = new TextField(k.github_url);
        TextField dokumanlarRepoYolu = new TextField(k.dokumanlar_repo_yolu);

        VBox root = new VBox(6,
                baslik("Arşiv"),
                new Label("GitHub Reposu"), githubUrl,
                new Label("Arşiv Kökü (JSON klasörüne göre)"), dokumanlarRepoYolu);

        root.getChildren().addAll(new Separator(), baslik("Google Formları"),
                new Label("Formlar kurulmadıysa boş bırak; README'lerde form linkleri yerine \"Henüz yorum yok.\" yazar."));
        TextArea yapistirAlani = new TextArea();
        yapistirAlani.setPromptText("form_olustur.gs → formAyarlari() çıktısını buraya yapıştır");
        yapistirAlani.setPrefRowCount(3);
        Button yapistirBtn = new Button("Yapıştırılanı Alanlara Doldur");
        root.getChildren().addAll(new Label("Form ayarlarını yapıştır"), yapistirAlani, yapistirBtn);

        Map<String, TextField> alanlar = new LinkedHashMap<>();
        for (var form : FORMLAR.entrySet()) {
            root.getChildren().add(altBaslik(FORM_ADLARI.get(form.getKey())));
            TextField link = new TextField(k.formAlani(form.getKey(), "link"));
            alanlar.put(form.getKey() + ".link", link);
            root.getChildren().addAll(new Label(FORM_ADLARI.get(form.getKey()) + " linki"), link);
            for (var alan : form.getValue().entrySet()) {
                TextField t = new TextField(k.formAlani(form.getKey(), alan.getKey()));
                alanlar.put(form.getKey() + "." + alan.getKey(), t);
                root.getChildren().addAll(new Label(alan.getValue()), t);
            }
        }
        yapistirBtn.setOnAction(e -> {
            try {
                Map<String, String> degerler = formAyarlariniOku(yapistirAlani.getText());
                degerler.forEach((anahtar, deger) -> {
                    if (alanlar.containsKey(anahtar) && !deger.isBlank()) {
                        alanlar.get(anahtar).setText(deger);
                    }
                });
                Toast.show(stage, degerler.size() + " alan dolduruldu; kaydetmeyi unutma.", Toast.Tip.BASARI);
            } catch (IllegalArgumentException ex) {
                Dialogs.error("Okunamadı", ex.getMessage());
            }
        });

        TextArea mufredatSecenekleri = new TextArea(String.join("\n", k.mufredat.secenekler));
        mufredatSecenekleri.setPrefRowCount(4);
        TextField eskiOnek = new TextField(k.mufredat.eski_mufredat_oneki);
        TextField eskiYil = new TextField(k.mufredat.eski_mufredat_notu_yili);
        TextField eskiNot = new TextField(k.mufredat.eski_mufredat_notu);
        root.getChildren().addAll(new Separator(), baslik("Müfredat"),
                new Label("Müfredat Seçenekleri (her satıra bir tane; ilki yeni derslerin varsayılanı)"), mufredatSecenekleri,
                new Label("Eski Müfredat Öneki (boşsa eski müfredat notu yazılmaz)"), eskiOnek,
                new Label("Eski Müfredat Notu Başlangıç Yılı (ör. 2026-2027)"), eskiYil,
                new Label("Eski Müfredat Notu"), eskiNot);

        Button kaydetBtn = new Button("Kaydet");
        kaydetBtn.setOnAction(e -> {
            List<String> secenekler = Arrays.stream(mufredatSecenekleri.getText().split("\n"))
                    .map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
            if (secenekler.isEmpty()) {
                Dialogs.error("Hata", "En az bir müfredat seçeneği olmalı.");
                return;
            }
            k.github_url = githubUrl.getText().strip();
            k.dokumanlar_repo_yolu = dokumanlarRepoYolu.getText().strip();
            alanlar.forEach((anahtar, alan) -> {
                String[] p = anahtar.split("\\.", 2);
                formAlaniYaz(k, p[0], p[1], alan.getText().strip());
            });
            k.mufredat.secenekler = secenekler;
            k.mufredat.eski_mufredat_oneki = eskiOnek.getText().strip();
            k.mufredat.eski_mufredat_notu_yili = eskiYil.getText().strip();
            k.mufredat.eski_mufredat_notu = eskiNot.getText().strip();
            JsonUtil.write(konfYolu, k);
            Dialogs.info("Kaydedildi", "Konfigürasyon güncellendi. README'lere yansıması için Git İşlemleri › Readme Güncelle.");
            stage.close();
        });
        root.getChildren().addAll(new Separator(), kaydetBtn);
        root.setPadding(new Insets(10));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 640, 700));
        stage.showAndWait();
    }

    /**
     * formAyarlari() çıktısını ("hoca_yorumlama": {"link": …, "hoca_alani": …}, …) "form.alan" → değer
     * eşlemesine çevirir. Günlükteki açıklama satırı da yapıştırılmışsa yalnızca JSON kısmı okunur.
     */
    static Map<String, String> formAyarlariniOku(String metin) {
        int bas = metin.indexOf('{');
        int son = metin.lastIndexOf('}');
        if (bas < 0 || son < bas) {
            throw new IllegalArgumentException("Yapıştırılan metinde JSON bulunamadı.");
        }
        JsonNode kok;
        try {
            kok = JsonUtil.MAPPER.readTree(metin.substring(bas, son + 1));
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON okunamadı: " + e.getMessage());
        }
        Map<String, String> sonuc = new LinkedHashMap<>();
        for (String form : FORMLAR.keySet()) {
            JsonNode f = kok.get(form);
            if (f == null || !f.isObject()) {
                continue;
            }
            f.fields().forEachRemaining(e -> sonuc.put(form + "." + e.getKey(), e.getValue().asText()));
        }
        if (sonuc.isEmpty()) {
            throw new IllegalArgumentException("Metinde hoca_yorumlama, ders_yorumlama ya da ders_oylama yok.");
        }
        return sonuc;
    }

    /** formlar.<form>.<alan> = değer; form yoksa oluşturur, formdaki diğer anahtarları (ör. "not") korur. */
    @SuppressWarnings("unchecked")
    static void formAlaniYaz(Konfigurasyon k, String form, String alan, String deger) {
        Object f = k.formlar.get(form);
        Map<String, Object> m;
        if (f instanceof Map<?, ?>) {
            m = (Map<String, Object>) f;
        } else {
            if (deger.isEmpty()) {
                return;
            }
            m = new LinkedHashMap<>();
            k.formlar.put(form, m);
        }
        // Boş değer için yeni anahtar açılmaz (dosyada gereksiz "" alanlar birikmesin).
        if (!deger.isEmpty() || m.containsKey(alan)) {
            m.put(alan, deger);
        }
    }

    private static Map<String, String> alanlar(String... ciftler) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < ciftler.length; i += 2) {
            m.put(ciftler[i], ciftler[i + 1]);
        }
        return m;
    }

    private static Label baslik(String metin) {
        Label l = new Label(metin);
        l.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return l;
    }

    private static Label altBaslik(String metin) {
        Label l = new Label(metin);
        l.setStyle("-fx-font-weight: bold; -fx-padding: 6 0 0 0;");
        return l;
    }
}
