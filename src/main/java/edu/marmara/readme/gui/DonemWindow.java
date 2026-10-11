package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.model.DerslerBolumu;
import edu.marmara.readme.engine.model.Donem;
import edu.marmara.readme.engine.model.DonemlerBolumu;
import edu.marmara.readme.engine.model.Havuz;
import edu.marmara.readme.engine.util.JsonUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dönemleri (ör. 1-1 … 4-2) ve seçmeli havuzlarını düzenler: dönem README başlığı ve genel tavsiyeler,
 * havuz README başlığı ve açıklaması. Sıra, ana README'deki bölüm sırasıdır.
 */
public class DonemWindow {

    private final Path donemlerJsonYolu;
    private final Path derslerJsonYolu;
    private DonemlerBolumu data;
    private final ObservableList<Donem> donemModel = FXCollections.observableArrayList();
    private final ObservableList<Havuz> havuzModel = FXCollections.observableArrayList();
    private Stage stage;

    public DonemWindow(Path jsonDeposu) {
        this.donemlerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DONEMLER_JSON);
        this.derslerJsonYolu = jsonDeposu.resolve(ReadmeGenerator.DERSLER_JSON);
    }

    public void show() {
        data = JsonUtil.read(donemlerJsonYolu, DonemlerBolumu.class);
        if (data == null) {
            Dialogs.error("Hata", "donemler.json bulunamadı. JSON klasörünü kontrol edin.");
            return;
        }
        stage = new Stage();
        stage.setTitle("Dönem ve Seçmeli Havuzlarını Düzenle");
        stage.initModality(Modality.APPLICATION_MODAL);

        ListView<Donem> donemList = new ListView<>(donemModel);
        donemList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Donem d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.klasor + "  ›  " + d.donem_adi
                        + "   (" + d.genel_tavsiyeler.size() + " tavsiye)");
            }
        });
        donemList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && donemList.getSelectionModel().getSelectedItem() != null) {
                donemDuzenle(donemList.getSelectionModel().getSelectedItem());
            }
        });
        Button donemEkleBtn = new Button("➕ Dönem Ekle");
        donemEkleBtn.setOnAction(e -> donemDuzenle(null));

        ListView<Havuz> havuzList = new ListView<>(havuzModel);
        havuzList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Havuz h, boolean empty) {
                super.updateItem(h, empty);
                setText(empty || h == null ? null : h.klasor + "  ›  " + h.ad);
            }
        });
        havuzList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && havuzList.getSelectionModel().getSelectedItem() != null) {
                havuzDuzenle(havuzList.getSelectionModel().getSelectedItem());
            }
        });
        Button havuzEkleBtn = new Button("➕ Havuz Ekle");
        havuzEkleBtn.setOnAction(e -> havuzDuzenle(null));

        VBox root = new VBox(8,
                new Label("Dönemler (düzenlemek için çift tıkla)"), donemList, donemEkleBtn,
                new Label("Seçmeli Havuzları"), havuzList, havuzEkleBtn);
        root.setPadding(new Insets(10));
        VBox.setVgrow(donemList, Priority.ALWAYS);
        stage.setScene(new Scene(root, 560, 600));
        yenile();
        stage.showAndWait();
    }

    private void yenile() {
        donemModel.setAll(data.donemler);
        havuzModel.setAll(data.havuzlar);
    }

    private void kaydet() {
        JsonUtil.write(donemlerJsonYolu, data);
        yenile();
    }

    private boolean klasorKullaniliyor(String klasor, Object haric) {
        return data.donemler.stream().anyMatch(d -> d != haric && d.klasor.equals(klasor))
                || data.havuzlar.stream().anyMatch(h -> h != haric && h.klasor.equals(klasor));
    }

    private long dersSayisi(String klasor) {
        DerslerBolumu dersler = JsonUtil.read(derslerJsonYolu, DerslerBolumu.class);
        return dersler == null ? 0 : dersler.dersler.stream().filter(d -> d.ustKlasor().equals(klasor)).count();
    }

    private void donemDuzenle(Donem donem) {
        Stage dlg = new Stage();
        dlg.initOwner(stage);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(donem != null ? "Dönem Düzenle" : "Dönem Ekle");

        TextField klasorAlani = new TextField(donem != null ? donem.klasor : "");
        klasorAlani.setPromptText("ör. 1-1");
        klasorAlani.setDisable(donem != null);
        TextField adAlani = new TextField(donem != null ? donem.donem_adi : "");
        adAlani.setPromptText("ör. 1. Sınıf Güz Dönemi");
        ComboBox<Integer> yilBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6));
        yilBox.setValue(donem != null ? donem.yil : 1);
        ComboBox<String> donemBox = new ComboBox<>(FXCollections.observableArrayList("Güz", "Bahar"));
        donemBox.setValue(donem != null ? donem.donem : "Güz");

        ObservableList<String> tavsiyeler = FXCollections.observableArrayList(
                donem != null ? donem.genel_tavsiyeler : List.of());
        ListView<String> tavsiyeListView = new ListView<>(tavsiyeler);
        tavsiyeListView.setPrefHeight(150);
        DragReorder.enable(tavsiyeListView, yeniSira -> {
        });
        Button ekleBtn = new Button("Tavsiye Ekle");
        ekleBtn.setOnAction(e -> Dialogs.textInput("Tavsiye", "Tavsiye metni:", "").ifPresent(yeni -> {
            if (!yeni.isBlank()) {
                tavsiyeler.add(yeni.strip());
            }
        }));
        Button duzenleBtn = new Button("Seçiliyi Düzenle");
        duzenleBtn.setOnAction(e -> {
            int i = tavsiyeListView.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                Dialogs.textInput("Tavsiye", "Tavsiye metni:", tavsiyeler.get(i)).ifPresent(yeni -> {
                    if (!yeni.isBlank()) {
                        tavsiyeler.set(i, yeni.strip());
                    }
                });
            }
        });
        Button silBtn = new Button("Seçiliyi Sil");
        silBtn.setOnAction(e -> {
            int i = tavsiyeListView.getSelectionModel().getSelectedIndex();
            if (i >= 0) {
                tavsiyeler.remove(i);
            }
        });

        Button kaydetBtn = new Button(donem != null ? "Değişiklikleri Kaydet" : "Ekle");
        kaydetBtn.setOnAction(e -> {
            String klasor = klasorAlani.getText().strip();
            String ad = adAlani.getText().strip();
            if (klasor.isEmpty() || ad.isEmpty() || klasor.contains("/")) {
                Dialogs.error("Hata", "Klasör (ör. 1-1) ve dönem adı boş olamaz; klasörde \"/\" olamaz.");
                return;
            }
            if (klasorKullaniliyor(klasor, donem)) {
                Dialogs.error("Hata", klasor + " klasörü zaten kullanılıyor.");
                return;
            }
            Donem hedef = donem != null ? donem : new Donem();
            hedef.klasor = klasor;
            hedef.donem_adi = ad;
            hedef.yil = yilBox.getValue();
            hedef.donem = donemBox.getValue();
            hedef.genel_tavsiyeler = new ArrayList<>(tavsiyeler);
            if (donem == null) {
                data.donemler.add(hedef);
            }
            kaydet();
            dlg.close();
        });
        HBox altButonlar = new HBox(8, kaydetBtn);
        if (donem != null) {
            Button donemSilBtn = new Button("Dönemi Sil");
            donemSilBtn.setOnAction(e -> {
                long n = dersSayisi(donem.klasor);
                if (n > 0) {
                    Dialogs.error("Silinemez", "Bu dönemde " + n + " ders var; önce dersleri taşı ya da sil.");
                } else if (Dialogs.confirm("Onay", "Dönem silinsin mi?")) {
                    data.donemler.remove(donem);
                    kaydet();
                    dlg.close();
                }
            });
            altButonlar.getChildren().add(donemSilBtn);
        }

        VBox root = new VBox(6,
                new Label("Klasör"), klasorAlani,
                new Label("README başlığı"), adAlani,
                new Label("Sınıf"), yilBox,
                new Label("Dönem"), donemBox,
                new Label("Genel Tavsiyeler (sürükleyerek sıralayabilirsin)"), tavsiyeListView,
                new HBox(8, ekleBtn, duzenleBtn, silBtn),
                altButonlar);
        root.setPadding(new Insets(12));
        dlg.setScene(new Scene(root, 520, 600));
        dlg.showAndWait();
    }

    private void havuzDuzenle(Havuz havuz) {
        Stage dlg = new Stage();
        dlg.initOwner(stage);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(havuz != null ? "Havuz Düzenle" : "Havuz Ekle");

        TextField klasorAlani = new TextField(havuz != null ? havuz.klasor : "");
        klasorAlani.setPromptText("ör. Mesleki Seçmeli");
        klasorAlani.setDisable(havuz != null);
        TextField adAlani = new TextField(havuz != null ? havuz.ad : "");
        TextArea aciklamaAlani = new TextArea(havuz != null ? havuz.aciklama : "");
        aciklamaAlani.setWrapText(true);
        aciklamaAlani.setPrefRowCount(4);

        Button kaydetBtn = new Button(havuz != null ? "Değişiklikleri Kaydet" : "Ekle");
        kaydetBtn.setOnAction(e -> {
            String klasor = klasorAlani.getText().strip();
            String ad = adAlani.getText().strip();
            if (klasor.isEmpty() || ad.isEmpty() || klasor.contains("/")) {
                Dialogs.error("Hata", "Klasör ve havuz adı boş olamaz; klasörde \"/\" olamaz.");
                return;
            }
            if (klasorKullaniliyor(klasor, havuz)) {
                Dialogs.error("Hata", klasor + " klasörü zaten kullanılıyor.");
                return;
            }
            Havuz hedef = havuz != null ? havuz : new Havuz();
            hedef.klasor = klasor;
            hedef.ad = ad;
            hedef.aciklama = aciklamaAlani.getText().strip();
            if (havuz == null) {
                data.havuzlar.add(hedef);
            }
            kaydet();
            dlg.close();
        });
        HBox altButonlar = new HBox(8, kaydetBtn);
        if (havuz != null) {
            Button havuzSilBtn = new Button("Havuzu Sil");
            havuzSilBtn.setOnAction(e -> {
                long n = dersSayisi(havuz.klasor);
                if (n > 0) {
                    Dialogs.error("Silinemez", "Bu havuzda " + n + " ders var; önce dersleri taşı ya da sil.");
                } else if (Dialogs.confirm("Onay", "Havuz silinsin mi?")) {
                    data.havuzlar.remove(havuz);
                    kaydet();
                    dlg.close();
                }
            });
            altButonlar.getChildren().add(havuzSilBtn);
        }

        VBox root = new VBox(6,
                new Label("Klasör"), klasorAlani,
                new Label("README başlığı"), adAlani,
                new Label("Açıklama (havuz README'sinde başlığın altında)"), aciklamaAlani,
                altButonlar);
        root.setPadding(new Insets(12));
        dlg.setScene(new Scene(root, 520, 380));
        dlg.showAndWait();
    }
}
