package edu.marmara.readme.gui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ekransız (Monocle) arayüz testleri için kullanıcı gibi davranan yardımcılar: pencere bekler, alan doldurur,
 * düğmeye basar. Modal pencereler (showAndWait) JavaFX iş parçacığını iç içe bir olay döngüsünde bekletir;
 * pencere açan düğmeler bu yüzden {@link #bas} ile beklemeden tetiklenir.
 */
final class ArayuzRobotu {

    private static final long ZAMAN_ASIMI_MS = 15_000;
    private static boolean basladi;

    private ArayuzRobotu() {
    }

    /** JavaFX'i ekransız başlatır (JVM başına bir kez). */
    static synchronized void baslat() throws InterruptedException {
        if (basladi) {
            return;
        }
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        CountDownLatch hazir = new CountDownLatch(1);
        Platform.startup(hazir::countDown);
        assertTrue(hazir.await(20, TimeUnit.SECONDS), "JavaFX başlamadı");
        Platform.setImplicitExit(false);
        basladi = true;
    }

    /** İşi JavaFX iş parçacığında çalıştırır ve bitmesini bekler; pencere açmayan işler için. */
    static <T> T fx(Callable<T> is) {
        AtomicReference<T> sonuc = new AtomicReference<>();
        AtomicReference<Throwable> hata = new AtomicReference<>();
        CountDownLatch bitti = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                sonuc.set(is.call());
            } catch (Throwable t) {
                hata.set(t);
            } finally {
                bitti.countDown();
            }
        });
        try {
            if (!bitti.await(ZAMAN_ASIMI_MS, TimeUnit.MILLISECONDS)) {
                fail("JavaFX işi zaman aşımına uğradı; açık pencereler: " + acikPencereler());
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        if (hata.get() != null) {
            throw new AssertionError(hata.get());
        }
        return sonuc.get();
    }

    static void fx(Runnable is) {
        fx(() -> {
            is.run();
            return null;
        });
    }

    /** Pencere açan (showAndWait ile bekleyen) işi başlatır; kapandığında sayacı düşer. */
    static CountDownLatch arkaPlanda(Runnable is) {
        CountDownLatch bitti = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                is.run();
            } finally {
                bitti.countDown();
            }
        });
        return bitti;
    }

    /** Başlığı tam olarak bu olan açık pencereyi bekler. */
    static Window pencere(String baslik) {
        long son = System.currentTimeMillis() + ZAMAN_ASIMI_MS;
        while (System.currentTimeMillis() < son) {
            Window w = fx(() -> Window.getWindows().stream()
                    .filter(x -> x instanceof Stage s && baslik.equals(s.getTitle()) && s.isShowing())
                    .reduce((a, b) -> b).orElse(null));
            if (w != null) {
                return w;
            }
            uyu(50);
        }
        throw new AssertionError("\"" + baslik + "\" penceresi açılmadı; açık pencereler: " + acikPencereler());
    }

    static void kapanmasiniBekle(Window w) {
        long son = System.currentTimeMillis() + ZAMAN_ASIMI_MS;
        while (System.currentTimeMillis() < son) {
            if (!fx(w::isShowing)) {
                return;
            }
            uyu(50);
        }
        throw new AssertionError("Pencere kapanmadı; açık pencereler: " + acikPencereler());
    }

    static void bekle(CountDownLatch l) throws InterruptedException {
        if (!l.await(ZAMAN_ASIMI_MS, TimeUnit.MILLISECONDS)) {
            fail("Pencere kapanmadı; açık pencereler: " + acikPencereler());
        }
    }

    static List<String> acikPencereler() {
        List<String> adlar = new ArrayList<>();
        CountDownLatch l = new CountDownLatch(1);
        Platform.runLater(() -> {
            for (Window w : Window.getWindows()) {
                if (w instanceof Stage s) {
                    adlar.add(s.getTitle() + (s.getScene().getRoot() instanceof DialogPane dp
                            ? " [" + dp.getContentText() + "]" : ""));
                }
            }
            l.countDown();
        });
        try {
            l.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        return adlar;
    }

    /** Testten sonra açık kalan pencereleri kapatır (bir test yarıda kalırsa diğerleri etkilenmesin). */
    static void hepsiniKapat() {
        for (int i = 0; i < 20; i++) {
            boolean kaldi = fx(() -> {
                List<Window> acik = new ArrayList<>(Window.getWindows());
                acik.forEach(w -> {
                    if (w instanceof Stage s) {
                        s.close();
                    } else {
                        w.hide();
                    }
                });
                return !acik.isEmpty();
            });
            if (!kaldi) {
                return;
            }
            uyu(50);
        }
    }

    // ------------------------------------------------------------ düğüm arama

    private static void topla(Node n, List<Node> liste) {
        liste.add(n);
        if (n instanceof ScrollPane sp) {
            if (sp.getContent() != null) {
                topla(sp.getContent(), liste);
            }
        } else if (n instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> topla(c, liste));
        }
    }

    private static List<Node> dugumler(Window w) {
        List<Node> liste = new ArrayList<>();
        topla(w.getScene().getRoot(), liste);
        return liste;
    }

    /** Penceredeki ilk eşleşen düğüm; JavaFX iş parçacığında çağrılmalı. */
    static <T extends Node> T bul(Window w, Class<T> tip, Predicate<T> kosul) {
        return dugumler(w).stream().filter(tip::isInstance).map(tip::cast).filter(kosul).findFirst()
                .orElseThrow(() -> new AssertionError(tip.getSimpleName() + " bulunamadı"));
    }

    static <T extends Node> T bul(Window w, Class<T> tip) {
        return bul(w, tip, x -> true);
    }

    /** Metni bu olan etiketin hemen altındaki (aynı kutudaki bir sonraki) düğüm; JavaFX iş parçacığında. */
    @SuppressWarnings("unchecked")
    static <T extends Node> T etiketinAltindaki(Window w, String etiket) {
        Label l = bul(w, Label.class, x -> etiket.equals(x.getText()));
        List<Node> kardesler = l.getParent().getChildrenUnmodifiable();
        return (T) kardesler.get(kardesler.indexOf(l) + 1);
    }

    /** Düğmeye basar, beklemeden döner (düğme yeni bir modal pencere açabilir). */
    static void bas(Window w, String dugmeMetni) {
        Button b = fx(() -> bul(w, Button.class, x -> dugmeMetni.equals(x.getText())));
        Platform.runLater(b::fire);
    }

    /** Alert/TextInputDialog/ChoiceDialog penceresinde bu düğmeye basar. */
    static void diyalogDugmesi(Window w, ButtonType tip) {
        Button b = fx(() -> (Button) ((DialogPane) w.getScene().getRoot()).lookupButton(tip));
        Platform.runLater(b::fire);
        kapanmasiniBekle(w);
    }

    /** Başlığı bu olan metin giriş diyaloğuna yazıp Tamam'a basar. */
    static void metinGir(String diyalogBasligi, String metin) {
        Window d = pencere(diyalogBasligi);
        fx(() -> bul(d, TextField.class).setText(metin));
        diyalogDugmesi(d, ButtonType.OK);
    }

    private static void uyu(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
