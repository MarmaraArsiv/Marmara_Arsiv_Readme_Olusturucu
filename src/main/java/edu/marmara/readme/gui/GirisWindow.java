package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.GirisBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ana README'nin başı: başlık, açıklama, geri bildirim paragrafı, Katkıda Bulunun bölümü ve sayaç. */
public class GirisWindow {

    private final Path girisJsonYolu;
    private GirisBolumu data;
    private Stage stage;

    public GirisWindow(Path jsonDeposu) {
        this.girisJsonYolu = jsonDeposu.resolve(ReadmeGenerator.GIRIS_JSON);
    }

    public void show() {
        data = JsonUtil.read(girisJsonYolu, GirisBolumu.class);
        if (data == null) {
            data = new GirisBolumu();
            data.baslik = Sabitler.VARSAYILAN_GIRIS_BASLIK;
            data.aciklama = Sabitler.VARSAYILAN_GIRIS_ACIKLAMA;
            jsonKaydet();
        }

        stage = new Stage();
        stage.setTitle("Giriş Güncelle");
        stage.initModality(Modality.APPLICATION_MODAL);

        Button baslikBtn = new Button();
        baslikBtn.setMaxWidth(Double.MAX_VALUE);
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Başlık", "Giriş başlığı:", data.baslik).ifPresent(yeni -> {
            data.baslik = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
            Toast.show(stage, "Başlık güncellendi.", Toast.Tip.BASARI);
        }));

        Button aciklamaBtn = new Button();
        aciklamaBtn.setMaxWidth(Double.MAX_VALUE);
        aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
        aciklamaBtn.setOnAction(e -> Dialogs.multilineInput("Açıklama", "Giriş açıklaması:", data.aciklama).ifPresent(yeni -> {
            data.aciklama = yeni;
            jsonKaydet();
            aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
            Toast.show(stage, "Açıklama güncellendi.", Toast.Tip.BASARI);
        }));

        Button geriBildirimBtn = metinButonu("Geri bildirim paragrafı",
                "\"Geri Bildirimde Bulunun\" başlığının altındaki paragraf (form linkleri konfigürasyondan gelir):",
                () -> data.geri_bildirim_aciklamasi, s -> data.geri_bildirim_aciklamasi = s);
        Button katkidaBulununBtn = metinButonu("Katkıda Bulunun bölümü",
                "\"## 🤝 Katkıda Bulunun\" bölümünün tamamı, Video Rehberler dahil (markdown):",
                () -> data.katkida_bulunun, s -> data.katkida_bulunun = s);
        Button sayacBtn = metinButonu("Görüntülenme sayacı",
                "Sayaç HTML'i. Adresteki kullanıcı adını değiştirirsen sayım sıfırdan başlar:",
                () -> data.sayac, s -> data.sayac = s);

        VBox root = new VBox(8, baslikBtn, aciklamaBtn, geriBildirimBtn, katkidaBulununBtn, sayacBtn);
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root, 520, 240));
        stage.showAndWait();
    }

    private void jsonKaydet() {
        JsonUtil.write(girisJsonYolu, data);
    }

    private Button metinButonu(String ad, String aciklama, Supplier<String> al, Consumer<String> yaz) {
        Button b = new Button(ad);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> Dialogs.multilineInput(ad, aciklama, al.get()).ifPresent(yeni -> {
            if (!yeni.equals(al.get())) {
                yaz.accept(yeni);
                jsonKaydet();
                Toast.show(stage, ad + " güncellendi.", Toast.Tip.BASARI);
            }
        }));
        return b;
    }
}
