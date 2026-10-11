package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.git.GitHelper;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

/**
 * Arşiv reposu için git işlemleri: GitHub'dan çek, README'leri JSON'dan üret, değişiklikleri seçip
 * commit'le ve pushla. Form cevapları (yıldızlar, onaylı yorumlar) GitHub'da otomatik işlenir.
 */
public class GitIslemleriWindow {

    private final Path jsonDeposu;

    public GitIslemleriWindow(Path jsonDeposu) {
        this.jsonDeposu = jsonDeposu;
    }

    public void show() {
        Path dokumanlarRepoYolu = ReadmeGenerator.repoKokuBul(jsonDeposu);

        Stage stage = new Stage();
        stage.setTitle("Git İşlemleri");
        stage.initModality(Modality.APPLICATION_MODAL);

        TextArea cikti = new TextArea();
        cikti.setEditable(false);
        cikti.setPrefRowCount(14);

        Button readmeGuncelleBtn = new Button("📝 Readme Güncelle");
        readmeGuncelleBtn.setOnAction(e -> {
            cikti.clear();
            ReadmeGenerator generator = new ReadmeGenerator(jsonDeposu, msg -> cikti.appendText(msg + "\n"));
            try {
                generator.generateAll();
                Toast.show(stage, "README.md dosyaları güncellendi.", Toast.Tip.BASARI);
            } catch (Exception ex) {
                Dialogs.error("Hata", "README üretilirken hata oluştu: " + ex.getMessage());
            }
        });

        Button cekBtn = new Button("⬇️ Dosya Değişikliklerini GitHub'dan Çek");
        cekBtn.setOnAction(e -> {
            // Takip edilmeyen yeni dosyalar çekmeye engel değil; değiştirilmiş dosyalar önce pushlanmalı.
            boolean temiz = GitHelper.gitStatus(dokumanlarRepoYolu).stream().allMatch(s -> s.startsWith("??"));
            if (!temiz) {
                Dialogs.error("Hata", "Yerel değişiklikler var, önce bunları pushlayın veya geri alın.");
                return;
            }
            cikti.appendText("GitHub'dan çekiliyor...\n");
            var sonuc = GitHelper.gitPullRebase(dokumanlarRepoYolu);
            cikti.appendText(sonuc.basarili() ? "Başarıyla güncellendi.\n" : "Hata: " + sonuc.stderr() + "\n");
        });

        Button pushBtn = new Button("⬆️ Değişiklikleri GitHub'a Pushla");
        pushBtn.setOnAction(e -> pushDialogAc(dokumanlarRepoYolu));

        Button durumBtn = new Button("📋 Değişiklikleri Göster");
        durumBtn.setOnAction(e -> {
            List<String> durum = GitHelper.gitStatus(dokumanlarRepoYolu);
            cikti.setText(durum.isEmpty() ? "Değişiklik yok." : String.join("\n", durum));
        });

        Button hocaDersAdlariBtn = new Button("👥 Hoca/Ders Adlarını Al");
        hocaDersAdlariBtn.setOnAction(e -> new HocaDersAdlariWindow(jsonDeposu).show());

        // Form cevapları (yıldızlar, onaylı yorumlar) GitHub'da otomatik işlenir; burada ayrı bir düğme gerekmez.
        VBox root = new VBox(8, new Label("Sıra: GitHub'dan çek → düzenle → Readme Güncelle → değişiklikleri pushla"),
                cekBtn, readmeGuncelleBtn, durumBtn, hocaDersAdlariBtn, pushBtn, new Label("Çıktı:"), cikti);
        root.setPadding(new Insets(10));
        stage.setScene(new Scene(root, 640, 600));
        stage.showAndWait();
    }

    private record DegisenDosya(String durumKodu, String dosyaAdi) {
    }

    /**
     * GitHub'daki yeni commit'leri (botlarınkiler dahil) alıp yerel commit'leri üstüne dizer ve gönderir.
     * Hata olursa kullanıcıya gösterilecek mesajı, başarılıysa null döner. Commit'ler hiçbir durumda kaybolmaz.
     */
    static String cekVeGonder(Path repoPath) {
        var pull = GitHelper.gitPullRebase(repoPath);
        if (!pull.basarili()) {
            // Çoğunlukla botun aynı satırlara yazdığı yorum/yıldız bloğuyla çakışma: senin sürümün geçerli olur,
            // bot o blokları birkaç dakika içinde form cevaplarından yeniden yazar.
            GitHelper.gitRebaseAbort(repoPath);
            pull = GitHelper.gitPullRebaseYerelOncelikli(repoPath);
            if (!pull.basarili()) {
                GitHelper.gitRebaseAbort(repoPath);
                return "GitHub'dan çekilemedi, push yapılmadı. Commit'in yerelde duruyor; internet bağlantını "
                        + "kontrol edip tekrar Pushla'ya bas.\n\n" + pull.stderr();
            }
        }
        var push = GitHelper.gitPush(repoPath);
        return push.basarili() ? null : "Push başarısız. Commit'in yerelde duruyor; tekrar Pushla'ya basabilirsin.\n\n"
                + push.stderr();
    }

    private void pushDialogAc(Path repoPath) {
        List<String> durum = GitHelper.gitStatus(repoPath);
        if (durum.isEmpty()) {
            // Önceki push yarıda kaldıysa (ör. internet yoktu) commit yerelde bekliyor olabilir.
            int bekleyen = GitHelper.gonderilmemisCommitSayisi(repoPath);
            if (bekleyen == 0) {
                Dialogs.info("Bilgi", "Gönderilecek değişiklik yok.");
            } else if (Dialogs.confirm("Gönderilmemiş Commit", "Yerelde GitHub'a gönderilmemiş " + bekleyen
                    + " commit var. Şimdi gönderilsin mi?")) {
                String hata = cekVeGonder(repoPath);
                if (hata == null) {
                    Dialogs.info("Tamam", "Commit'ler GitHub'a gönderildi.");
                } else {
                    Dialogs.error("Hata", hata);
                }
            }
            return;
        }

        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Değişiklikleri Pushlama");

        ListView<CheckBox> dosyaListView = new ListView<>();
        for (String satir : durum) {
            String kod = satir.substring(0, 2);
            String dosyaAdi = satir.substring(3);
            CheckBox cb = new CheckBox(kod + "  " + dosyaAdi);
            cb.setSelected(true);
            cb.setUserData(new DegisenDosya(kod, dosyaAdi));
            dosyaListView.getItems().add(cb);
        }

        Button farkiGorBtn = new Button("Farkı Gör");
        farkiGorBtn.setOnAction(e -> {
            CheckBox secili = dosyaListView.getSelectionModel().getSelectedItem();
            if (secili == null) {
                return;
            }
            DegisenDosya dd = (DegisenDosya) secili.getUserData();
            String eski = GitHelper.getFileContentAtCommit(repoPath, dd.dosyaAdi(), "HEAD");
            String yeni = GitHelper.getFileContentAtCommit(repoPath, dd.dosyaAdi(), "WORKING");
            new DiffWindow(dd.dosyaAdi(), eski, yeni).show();
        });

        TextArea commitMesaji = new TextArea();
        commitMesaji.setPromptText("Commit mesajı...");
        commitMesaji.setPrefRowCount(3);

        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> {
            if (commitMesaji.getText().isBlank()) {
                Dialogs.error("Hata", "Commit mesajı gir.");
                return;
            }
            boolean hicSecilenYok = dosyaListView.getItems().stream().noneMatch(CheckBox::isSelected);
            if (hicSecilenYok) {
                Dialogs.error("Hata", "Pushlanacak hiçbir dosya seçili değil.");
                return;
            }
            if (!Dialogs.confirm("Emin Misiniz?", "Değişiklikleri pushlamak istediğinize emin misiniz?")) {
                return;
            }
            for (CheckBox cb : dosyaListView.getItems()) {
                DegisenDosya dd = (DegisenDosya) cb.getUserData();
                if (cb.isSelected()) {
                    GitHelper.gitAdd(repoPath, dd.dosyaAdi());
                }
            }
            var commit = GitHelper.gitCommit(repoPath, commitMesaji.getText());
            if (!commit.basarili() && !commit.stderr().isBlank()) {
                Dialogs.error("Hata", "Commit başarısız: " + commit.stderr());
                return;
            }
            // GitHub'daki botlar sık commit atar; önce onları alıp kendi commit'ini üstüne diz.
            String hata = cekVeGonder(repoPath);
            if (hata == null) {
                Toast.show(dlg, "Değişiklikler push edildi.", Toast.Tip.BASARI);
                dlg.close();
            } else {
                Dialogs.error("Hata", hata);
            }
        });

        VBox root = new VBox(8, new Label("Değişiklikler (işaretli olanlar commit'e dahil edilir):"),
                dosyaListView, farkiGorBtn, new Label("Commit Mesajı"), commitMesaji, okBtn);
        root.setPadding(new Insets(12));
        VBox.setVgrow(dosyaListView, Priority.ALWAYS);
        dlg.setScene(new Scene(root, 520, 560));
        dlg.showAndWait();
    }
}
