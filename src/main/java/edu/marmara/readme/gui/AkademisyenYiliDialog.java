package edu.marmara.readme.gui;

import edu.marmara.readme.engine.model.AkademisyenYili;
import edu.marmara.readme.engine.model.DersHocasi;
import edu.marmara.readme.engine.model.HocalarBolumu;
import edu.marmara.readme.engine.util.TurkceSira;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bir dersin tek bir akademik yılda kimler tarafından yürütüldüğünü düzenler. */
public class AkademisyenYiliDialog {

    private final Window owner;
    private final AkademisyenYili mevcut;
    private final String onerilenYil;
    private final HocalarBolumu hocalar;
    private final List<AkademisyenYili> tumYillar;
    private final List<DersHocasi> liste = new ArrayList<>();
    private final ObservableList<String> gosterim = FXCollections.observableArrayList();
    private AkademisyenYili sonuc;

    /** mevcut null ise yeni yıl eklenir; tumYillar aynı yılın iki kez eklenmesini önlemek için. */
    public AkademisyenYiliDialog(Window owner, AkademisyenYili mevcut, String onerilenYil, HocalarBolumu hocalar,
                                 List<AkademisyenYili> tumYillar) {
        this.owner = owner;
        this.mevcut = mevcut;
        this.onerilenYil = onerilenYil;
        this.hocalar = hocalar;
        this.tumYillar = tumYillar;
    }

    public Optional<AkademisyenYili> show() {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(mevcut != null ? "Yılı Düzenle" : "Yıl Ekle");

        TextField yilAlani = new TextField(mevcut != null ? mevcut.yil : onerilenYil);
        yilAlani.setPromptText("ör. 2026-2027");
        if (mevcut != null) {
            mevcut.hocalar.forEach(h -> liste.add(new DersHocasi(h.ad, h.not)));
        }
        ListView<String> listView = new ListView<>(gosterim);
        listView.setPrefHeight(180);
        goster();

        Button listedenBtn = new Button("Listeden Hoca Ekle");
        listedenBtn.setOnAction(e -> {
            List<String> adlar = new ArrayList<>(hocalar.hocalar.stream().map(h -> h.ad).toList());
            adlar.sort((a, b) -> TurkceSira.karsilastir(TurkceSira.unvansiz(a), TurkceSira.unvansiz(b)));
            if (adlar.isEmpty()) {
                Dialogs.error("Hata", "Hoca listesi boş. Önce Hoca Ekle/Güncelle'den hoca ekleyin.");
                return;
            }
            Dialogs.choice("Hoca Seç", "Hoca:", adlar, adlar.get(0)).ifPresent(this::ekle);
        });
        Button disaridanBtn = new Button("Listede Olmayan İsim");
        disaridanBtn.setOnAction(e -> Dialogs.textInput("İsim",
                "Hoca listesinde olmayan biri (README'de kısaltmasız, düz metin yazılır):", "")
                .ifPresent(ad -> {
                    if (!ad.isBlank()) {
                        ekle(ad.strip());
                    }
                }));
        Button notBtn = new Button("Not Ekle/Değiştir");
        notBtn.setOnAction(e -> {
            int i = listView.getSelectionModel().getSelectedIndex();
            if (i < 0) {
                return;
            }
            Dialogs.textInput("Not", "Adın yanında parantez içinde görünür, ör. lab, atölye (boş bırakılabilir):",
                    liste.get(i).not).ifPresent(not -> {
                liste.get(i).not = not.strip();
                goster();
                listView.getSelectionModel().select(i);
            });
        });
        Button kaldirBtn = new Button("Kaldır");
        kaldirBtn.setOnAction(e -> {
            int i = listView.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                liste.remove(i);
                goster();
            }
        });
        Button yukariBtn = new Button("⬆️");
        yukariBtn.setOnAction(e -> tasi(listView, -1));
        Button asagiBtn = new Button("⬇️");
        asagiBtn.setOnAction(e -> tasi(listView, 1));

        Button tamamBtn = new Button("Tamam");
        tamamBtn.setOnAction(e -> {
            String yil = yilAlani.getText().strip();
            if (!yilGecerli(yil)) {
                Dialogs.error("Hata", "Yıl \"2026-2027\" biçiminde olmalı (ikinci yıl birincinin bir fazlası).");
                return;
            }
            if (tumYillar.stream().anyMatch(y -> y != mevcut && y.yil.equals(yil))) {
                Dialogs.error("Hata", yil + " zaten var; onu düzenleyin.");
                return;
            }
            if (liste.isEmpty()) {
                Dialogs.error("Hata", "En az bir hoca ekleyin.");
                return;
            }
            sonuc = new AkademisyenYili();
            sonuc.yil = yil;
            sonuc.hocalar = new ArrayList<>(liste);
            stage.close();
        });
        Button iptalBtn = new Button("İptal");
        iptalBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(6,
                new Label("Akademik Yıl"), yilAlani,
                new Label("Hocalar (README'de bu sırayla)"), listView,
                new HBox(6, listedenBtn, disaridanBtn),
                new HBox(6, notBtn, kaldirBtn, yukariBtn, asagiBtn),
                new HBox(8, tamamBtn, iptalBtn));
        root.setPadding(new Insets(12));
        stage.setScene(new Scene(root, 520, 420));
        stage.showAndWait();
        return Optional.ofNullable(sonuc);
    }

    private void ekle(String ad) {
        if (liste.stream().anyMatch(h -> h.ad.equals(ad))) {
            return;
        }
        liste.add(new DersHocasi(ad, ""));
        goster();
    }

    private void tasi(ListView<String> listView, int yon) {
        int i = listView.getSelectionModel().getSelectedIndex();
        int j = i + yon;
        if (i < 0 || j < 0 || j >= liste.size()) {
            return;
        }
        liste.add(j, liste.remove(i));
        goster();
        listView.getSelectionModel().select(j);
    }

    private void goster() {
        gosterim.setAll(liste.stream().map(h -> h.not.isEmpty() ? h.ad : h.ad + " (" + h.not + ")").toList());
    }

    static boolean yilGecerli(String yil) {
        if (!yil.matches("\\d{4}-\\d{4}")) {
            return false;
        }
        return Integer.parseInt(yil.substring(5)) == Integer.parseInt(yil.substring(0, 4)) + 1;
    }
}
