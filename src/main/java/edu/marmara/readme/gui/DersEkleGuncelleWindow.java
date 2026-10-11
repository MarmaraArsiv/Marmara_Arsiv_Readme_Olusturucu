package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.Ders;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
import edu.marmara.readme.engine.writer.Yardimci;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

/** Python ders_ekle_guncelle_window.py'nin (DersEkleGuncelleWindow) Java/JavaFX portu. */
public class DersEkleGuncelleWindow {

    private final Path derslerJsonYolu;
    private final Path hocalarJsonYolu;

    private DerslerBolumu data;
    private final ObservableList<Ders> dersListModel = FXCollections.observableArrayList();
    private final UndoManager<Ders> undoManager = new UndoManager<>();

    private ListView<Ders> listView;
    private Label sayiLabel;
    private TextField aramaAlani;
    private Stage stage;

    public DersEkleGuncelleWindow(Path jsonDeposu) {
        this.derslerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DERSLER_JSON);
        this.hocalarJsonYolu = jsonDeposu.resolve(ReadmeGenerator.HOCALAR_JSON);
    }

    public void show() {
        data = JsonUtil.read(derslerJsonYolu, DerslerBolumu.class);
        if (data == null) {
            data = new DerslerBolumu();
            data.bolum_adi = Sabitler.VARSAYILAN_DERS_BOLUM_ADI;
            data.bolum_aciklamasi = Sabitler.VARSAYILAN_DERS_BOLUM_ACIKLAMASI;
            jsonKaydet();
        }

        stage = new Stage();
        stage.setTitle("Ders Ekle/Güncelle");
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(8);
        root.setPadding(new Insets(10));

        Button bolumAdiBtn = new Button();
        bolumAdiBtn.setMaxWidth(Double.MAX_VALUE);
        bolumAdiBtn.setText(data.bolum_adi);
        bolumAdiBtn.setOnAction(e -> Dialogs.textInput("Bölüm Adı", "Bölüm adı:", data.bolum_adi).ifPresent(yeni -> {
            if (!yeni.equals(data.bolum_adi)) {
                data.bolum_adi = yeni;
                jsonKaydet();
            }
            bolumAdiBtn.setText(data.bolum_adi);
        }));

        Button aciklamaBtn = new Button();
        aciklamaBtn.setMaxWidth(Double.MAX_VALUE);
        aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.bolum_aciklamasi));
        aciklamaBtn.setOnAction(e -> Dialogs.multilineInput("Bölüm Açıklaması", "Bölüm açıklaması:", data.bolum_aciklamasi)
                .ifPresent(yeni -> {
                    if (!yeni.equals(data.bolum_aciklamasi)) {
                        data.bolum_aciklamasi = yeni;
                        jsonKaydet();
                    }
                    aciklamaBtn.setText(MetinIslemleri.kisaltMetin(data.bolum_aciklamasi));
                }));

        Button ekleBtn = new Button("➕ Ders Ekle");
        ekleBtn.setOnAction(e -> dersDuzenlemeAc(null));

        aramaAlani = new TextField();
        aramaAlani.setPromptText("Ders ara...");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> uygulaFiltre(yeni));

        sayiLabel = new Label();

        listView = new ListView<>(dersListModel);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ders ders, boolean empty) {
                super.updateItem(ders, empty);
                if (empty || ders == null) {
                    setText(null);
                } else {
                    setText(ders.ad + "  [" + ders.klasor + "]");
                }
            }
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                dersDuzenlemeAc(listView.getSelectionModel().getSelectedItem());
            }
        });

        HBox ustSatir = new HBox(8, ekleBtn, aramaAlani);
        root.getChildren().addAll(bolumAdiBtn, aciklamaBtn, ustSatir, sayiLabel, listView);
        VBox.setVgrow(listView, Priority.ALWAYS);

        Scene scene = new Scene(root, 680, 620);
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                undoSil();
            }
        });
        stage.setScene(scene);
        dersleriYenile();
        stage.showAndWait();
    }

    private void undoSil() {
        var silinen = undoManager.popDeleted();
        if (silinen == null) {
            return;
        }
        int index = Math.min(silinen.index(), data.dersler.size());
        data.dersler.add(index, silinen.item());
        jsonKaydet();
        dersleriYenile();
        Toast.show(stage, "Ders geri alındı.", Toast.Tip.BASARI);
    }

    private void jsonKaydet() {
        JsonUtil.write(derslerJsonYolu, data);
    }

    private void dersleriYenile() {
        List<Ders> sirali = data.dersler.stream().sorted(Yardimci.DERS_SIRALAMA).toList();
        dersListModel.setAll(sirali);
        sayiLabel.setText("Toplam " + sirali.size() + " ders");
        if (!aramaAlani.getText().isEmpty()) {
            uygulaFiltre(aramaAlani.getText());
        }
    }

    private void uygulaFiltre(String sorgu) {
        List<Ders> tumDersler = data.dersler.stream().sorted(Yardimci.DERS_SIRALAMA).toList();
        if (sorgu == null || sorgu.isEmpty()) {
            dersListModel.setAll(tumDersler);
            sayiLabel.setText("Toplam " + tumDersler.size() + " ders");
            return;
        }
        String q = sorgu.replace("İ", "i").toLowerCase();
        List<Ders> filtreli = tumDersler.stream()
                .filter(d -> d.ad.replace("İ", "i").toLowerCase().contains(q)).toList();
        dersListModel.setAll(filtreli);
        sayiLabel.setText(filtreli.size() + " ders bulundu");
    }

    private void dersDuzenlemeAc(Ders ders) {
        var hocalar = JsonUtil.read(hocalarJsonYolu, edu.marmara.readme.engine.model.HocalarBolumu.class);
        DersDuzenlemeDialog dialog = new DersDuzenlemeDialog(stage, ders, data, hocalar,
                () -> {
                    jsonKaydet();
                    dersleriYenile();
                    Toast.show(stage, "Ders başarıyla kaydedildi.", Toast.Tip.BASARI);
                },
                silinenDers -> {
                    int index = data.dersler.indexOf(silinenDers);
                    undoManager.pushDeleted(index, silinenDers);
                    data.dersler.remove(silinenDers);
                    jsonKaydet();
                    dersleriYenile();
                    Toast.show(stage, "Ders silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
                });
        dialog.show();
    }
}
