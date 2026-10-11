package edu.marmara.readme.gui;

import edu.marmara.readme.engine.ReadmeGenerator;
import edu.marmara.readme.engine.Sabitler;
import edu.marmara.readme.engine.model.Kavram;
import edu.marmara.readme.engine.model.RepoKullanimiBolumu;
import edu.marmara.readme.engine.util.JsonUtil;
import edu.marmara.readme.engine.util.MetinIslemleri;
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
import java.util.ArrayList;
import java.util.List;

/** Python repo_kullanimi_window.py'nin Java/JavaFX portu — talimatlar, kavramlar, açıklamalar bölümleri. */
public class RepoKullanimiWindow {

    private final Path yolu;
    private RepoKullanimiBolumu data;
    private Stage stage;
    private final UndoManager<String> talimatUndo = new UndoManager<>();
    private final UndoManager<String> aciklamaUndo = new UndoManager<>();

    public RepoKullanimiWindow(Path jsonDeposu) {
        this.yolu = jsonDeposu.resolve(ReadmeGenerator.REPO_KULLANIMI_JSON);
    }

    public void show() {
        data = JsonUtil.read(yolu, RepoKullanimiBolumu.class);
        if (data == null) {
            data = new RepoKullanimiBolumu();
            data.baslik = Sabitler.VARSAYILAN_REPO_KULLANIMI_BOLUM_ADI;
            jsonKaydet();
        }

        stage = new Stage();
        stage.setTitle("Repo Kullanımı Düzenle");
        stage.initModality(Modality.APPLICATION_MODAL);

        Button baslikBtn = new Button();
        baslikBtn.setMaxWidth(Double.MAX_VALUE);
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Başlık", "Repo kullanımı başlığı:", data.baslik).ifPresent(yeni -> {
            data.baslik = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.baslik));
            Toast.show(stage, "Başlık güncellendi.", Toast.Tip.BASARI);
        }));

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Talimatlar", talimatlarSekmesi()));
        tabs.getTabs().add(new Tab("Kavramlar", kavramlarSekmesi()));
        tabs.getTabs().add(new Tab("Açıklamalar", aciklamalarSekmesi()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox root = new VBox(8, baslikBtn, tabs);
        root.setPadding(new Insets(10));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 600, 560);
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                if (talimatUndo.canUndo()) {
                    undoTalimat();
                }
            }
        });
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void jsonKaydet() {
        JsonUtil.write(yolu, data);
    }

    private ObservableList<String> talimatlarModel;
    private Label talimatSayiLabel;

    private void undoTalimat() {
        var silinen = talimatUndo.popDeleted();
        if (silinen == null) {
            return;
        }
        int index = Math.min(silinen.index(), data.talimatlar.size());
        data.talimatlar.add(index, silinen.item());
        talimatlarModel.setAll(data.talimatlar);
        jsonKaydet();
        talimatSayiLabel.setText("Toplam " + data.talimatlar.size() + " talimat");
        Toast.show(stage, "Talimat geri alındı.", Toast.Tip.BASARI);
    }

    private VBox talimatlarSekmesi() {
        talimatlarModel = FXCollections.observableArrayList(data.talimatlar);
        Button baslikBtn = new Button();
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.talimat));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Talimatlar Bölüm Adı", "Bölüm adı:", data.talimat).ifPresent(yeni -> {
            data.talimat = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.talimat));
        }));
        ListView<String> listView = new ListView<>(talimatlarModel);
        DragReorder.enable(listView, yeniSira -> {
            data.talimatlar = new ArrayList<>(yeniSira);
            jsonKaydet();
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String secili = listView.getSelectionModel().getSelectedItem();
                if (secili != null) {
                    Dialogs.multilineInput("Talimat Düzenle", "Talimat:", secili).ifPresent(yeni -> {
                        int idx = talimatlarModel.indexOf(secili);
                        talimatlarModel.set(idx, yeni);
                        data.talimatlar.set(idx, yeni);
                        jsonKaydet();
                        Toast.show(stage, "Talimat güncellendi.", Toast.Tip.BASARI);
                    });
                }
            }
        });
        TextField aramaAlani = new TextField();
        aramaAlani.setPromptText("Talimat ara...");
        talimatSayiLabel = new Label("Toplam " + data.talimatlar.size() + " talimat");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> {
            if (yeni.isEmpty()) {
                talimatlarModel.setAll(data.talimatlar);
                talimatSayiLabel.setText("Toplam " + data.talimatlar.size() + " talimat");
                return;
            }
            String q = yeni.replace("İ", "i").toLowerCase();
            List<String> filtreli = data.talimatlar.stream()
                    .filter(s -> s.replace("İ", "i").toLowerCase().contains(q)).toList();
            talimatlarModel.setAll(filtreli);
            talimatSayiLabel.setText(filtreli.size() + " talimat bulundu");
        });
        Button ekleBtn = new Button("➕ Talimat Ekle");
        ekleBtn.setOnAction(e -> Dialogs.multilineInput("Talimat Ekle", "Talimat:", "").ifPresent(yeni -> {
            if (!yeni.isBlank()) {
                talimatlarModel.add(yeni);
                data.talimatlar.add(yeni);
                jsonKaydet();
                talimatSayiLabel.setText("Toplam " + data.talimatlar.size() + " talimat");
                Toast.show(stage, "Talimat eklendi.", Toast.Tip.BASARI);
            }
        }));
        Button silBtn = new Button("Seçili Talimatı Sil");
        silBtn.setOnAction(e -> {
            String secili = listView.getSelectionModel().getSelectedItem();
            if (secili != null) {
                int idx = data.talimatlar.indexOf(secili);
                talimatUndo.pushDeleted(idx, secili);
                talimatlarModel.remove(secili);
                data.talimatlar.remove(secili);
                jsonKaydet();
                talimatSayiLabel.setText("Toplam " + data.talimatlar.size() + " talimat");
                Toast.show(stage, "Talimat silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
            }
        });
        VBox box = new VBox(8, baslikBtn, new HBox(8, ekleBtn, silBtn, aramaAlani), talimatSayiLabel, listView);
        box.setPadding(new Insets(8));
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }

    private VBox kavramlarSekmesi() {
        ObservableList<Kavram> model = FXCollections.observableArrayList(data.kavramlar);
        Button baslikBtn = new Button();
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.kavram));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Kavramlar Bölüm Adı", "Bölüm adı:", data.kavram).ifPresent(yeni -> {
            data.kavram = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.kavram));
        }));
        ListView<Kavram> listView = new ListView<>(model);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Kavram k, boolean empty) {
                super.updateItem(k, empty);
                setText(empty || k == null ? null : k.kavram);
            }
        });
        TextField aramaAlani = new TextField();
        aramaAlani.setPromptText("Kavram ara...");
        Label sayiLabel = new Label("Toplam " + data.kavramlar.size() + " kavram");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> {
            if (yeni.isEmpty()) {
                model.setAll(data.kavramlar);
                sayiLabel.setText("Toplam " + data.kavramlar.size() + " kavram");
                return;
            }
            String q = yeni.replace("İ", "i").toLowerCase();
            List<Kavram> filtreli = data.kavramlar.stream()
                    .filter(k -> k.kavram.replace("İ", "i").toLowerCase().contains(q)).toList();
            model.setAll(filtreli);
            sayiLabel.setText(filtreli.size() + " kavram bulundu");
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && listView.getSelectionModel().getSelectedItem() != null) {
                kavramDuzenle(listView.getSelectionModel().getSelectedItem(), model);
            }
        });
        Button ekleBtn = new Button("➕ Kavram Ekle");
        ekleBtn.setOnAction(e -> kavramDuzenle(null, model));
        Button silBtn = new Button("Seçili Kavramı Sil");
        silBtn.setOnAction(e -> {
            Kavram secili = listView.getSelectionModel().getSelectedItem();
            if (secili != null) {
                model.remove(secili);
                data.kavramlar.remove(secili);
                jsonKaydet();
                sayiLabel.setText("Toplam " + data.kavramlar.size() + " kavram");
                Toast.show(stage, "Kavram silindi.", Toast.Tip.BASARI);
            }
        });
        Button kavramAciklamasiBtn = new Button("Kavramlar açıklaması");
        kavramAciklamasiBtn.setOnAction(e -> Dialogs.multilineInput("Kavramlar açıklaması",
                "Kavram listesinin üstündeki paragraf (satır sonları korunur):", data.kavram_aciklamasi)
                .ifPresent(yeni -> {
                    data.kavram_aciklamasi = yeni.strip();
                    jsonKaydet();
                }));
        VBox box = new VBox(8, new HBox(8, baslikBtn, kavramAciklamasiBtn), new HBox(8, ekleBtn, silBtn, aramaAlani),
                sayiLabel, listView, new Label("Kavramlar README'de bu listedeki sırayla görünür."));
        box.setPadding(new Insets(8));
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }

    private void kavramDuzenle(Kavram kavram, ObservableList<Kavram> model) {
        Stage dlg = new Stage();
        dlg.initOwner(stage);
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle(kavram != null ? "Kavram Düzenle" : "Kavram Ekle");

        TextField adAlani = new TextField(kavram != null ? kavram.kavram : "");
        ObservableList<String> aciklamalar = FXCollections.observableArrayList(
                kavram != null ? kavram.aciklamalar : List.of());
        ListView<String> listView = new ListView<>(aciklamalar);
        listView.setPrefHeight(150);
        DragReorder.enable(listView, yeniSira -> {
        });
        UndoManager<String> localUndo = new UndoManager<>();
        dlg.setOnShown(ev -> dlg.getScene().setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                var silinen = localUndo.popDeleted();
                if (silinen != null) {
                    int idx = Math.min(silinen.index(), aciklamalar.size());
                    aciklamalar.add(idx, silinen.item());
                    Toast.show(dlg, "Açıklama geri alındı.", Toast.Tip.BASARI);
                }
            }
        }));
        Button ekleBtn = new Button("Açıklama Ekle");
        ekleBtn.setOnAction(e -> Dialogs.multilineInput("Açıklama Ekle", "Açıklama:", "").ifPresent(yeni -> {
            if (!yeni.isBlank()) {
                aciklamalar.add(yeni);
            }
        }));
        Button silBtn = new Button("Seçili Açıklamayı Sil (Ctrl+Z: geri al)");
        silBtn.setOnAction(e -> {
            String secili = listView.getSelectionModel().getSelectedItem();
            if (secili != null) {
                int idx = aciklamalar.indexOf(secili);
                localUndo.pushDeleted(idx, secili);
                aciklamalar.remove(secili);
            }
        });

        Button kaydetBtn = new Button(kavram != null ? "Kaydet" : "Ekle");
        kaydetBtn.setOnAction(e -> {
            if (adAlani.getText().isBlank()) {
                Dialogs.error("Hata", "Kavram adı boş olamaz.");
                return;
            }
            Kavram hedef = kavram != null ? kavram : new Kavram();
            hedef.kavram = adAlani.getText().strip();
            hedef.aciklamalar = new ArrayList<>(aciklamalar);
            if (kavram == null) {
                data.kavramlar.add(hedef);
                model.add(hedef);
            } else {
                model.set(model.indexOf(kavram), hedef);
            }
            jsonKaydet();
            dlg.close();
            Toast.show(stage, "Kavram kaydedildi.", Toast.Tip.BASARI);
        });

        VBox root = new VBox(6, new Label("Kavram Adı"), adAlani,
                new Label("Açıklamalar (sürükleyerek sıralayabilirsin)"), listView, new HBox(8, ekleBtn, silBtn), kaydetBtn);
        root.setPadding(new Insets(12));
        dlg.setScene(new Scene(root, 440, 480));
        dlg.showAndWait();
    }

    private VBox aciklamalarSekmesi() {
        ObservableList<String> model = FXCollections.observableArrayList(data.aciklamalar);
        Button baslikBtn = new Button();
        baslikBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
        baslikBtn.setOnAction(e -> Dialogs.textInput("Açıklamalar Bölüm Adı", "Bölüm adı:", data.aciklama).ifPresent(yeni -> {
            data.aciklama = yeni;
            jsonKaydet();
            baslikBtn.setText(MetinIslemleri.kisaltMetin(data.aciklama));
        }));
        ListView<String> listView = new ListView<>(model);
        TextField aramaAlani = new TextField();
        aramaAlani.setPromptText("Açıklama ara...");
        Label sayiLabel = new Label("Toplam " + data.aciklamalar.size() + " açıklama");
        aramaAlani.textProperty().addListener((obs, eski, yeni) -> {
            if (yeni.isEmpty()) {
                model.setAll(data.aciklamalar);
                sayiLabel.setText("Toplam " + data.aciklamalar.size() + " açıklama");
                return;
            }
            String q = yeni.replace("İ", "i").toLowerCase();
            List<String> filtreli = data.aciklamalar.stream()
                    .filter(s -> s.replace("İ", "i").toLowerCase().contains(q)).toList();
            model.setAll(filtreli);
            sayiLabel.setText(filtreli.size() + " açıklama bulundu");
        });
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String secili = listView.getSelectionModel().getSelectedItem();
                if (secili != null) {
                    Dialogs.multilineInput("Açıklama Düzenle", "Açıklama:", secili).ifPresent(yeni -> {
                        int idx = model.indexOf(secili);
                        model.set(idx, yeni);
                        data.aciklamalar.set(idx, yeni);
                        jsonKaydet();
                        Toast.show(stage, "Açıklama güncellendi.", Toast.Tip.BASARI);
                    });
                }
            }
        });
        Button ekleBtn = new Button("➕ Açıklama Ekle");
        ekleBtn.setOnAction(e -> Dialogs.multilineInput("Açıklama Ekle", "Açıklama:", "").ifPresent(yeni -> {
            if (!yeni.isBlank()) {
                model.add(yeni);
                data.aciklamalar.add(yeni);
                jsonKaydet();
                sayiLabel.setText("Toplam " + data.aciklamalar.size() + " açıklama");
                Toast.show(stage, "Açıklama eklendi.", Toast.Tip.BASARI);
            }
        }));
        Button silBtn = new Button("Seçili Açıklamayı Sil (Ctrl+Z: geri al)");
        silBtn.setOnAction(e -> {
            String secili = listView.getSelectionModel().getSelectedItem();
            if (secili != null) {
                int idx = data.aciklamalar.indexOf(secili);
                aciklamaUndo.pushDeleted(idx, secili);
                model.remove(secili);
                data.aciklamalar.remove(secili);
                jsonKaydet();
                sayiLabel.setText("Toplam " + data.aciklamalar.size() + " açıklama");
                Toast.show(stage, "Açıklama silindi. (Geri almak için Ctrl+Z)", Toast.Tip.BASARI);
            }
        });
        listView.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                var silinen = aciklamaUndo.popDeleted();
                if (silinen != null) {
                    int idx = Math.min(silinen.index(), data.aciklamalar.size());
                    data.aciklamalar.add(idx, silinen.item());
                    model.setAll(data.aciklamalar);
                    jsonKaydet();
                    sayiLabel.setText("Toplam " + data.aciklamalar.size() + " açıklama");
                    Toast.show(stage, "Açıklama geri alındı.", Toast.Tip.BASARI);
                }
            }
        });
        VBox box = new VBox(8, baslikBtn, new HBox(8, ekleBtn, silBtn, aramaAlani), sayiLabel, listView);
        box.setPadding(new Insets(8));
        VBox.setVgrow(listView, Priority.ALWAYS);
        return box;
    }
}
