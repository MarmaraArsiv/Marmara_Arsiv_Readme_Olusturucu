package edu.marmara.readme.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/** hocalar.json içindeki "hocalar" dizisinin bir öğesi. Ad, ünvanıyla birlikte saklanır: "Prof. Dr. Ad Soyad". */
public class Hoca extends JsonNesnesi {
    public String ad = "";
    /** README'deki kısa ad (ör. "BD"); ders kartlarında hocaya bağlantı olarak kullanılır. */
    public String kisaltma = "";
    /** Hocanın ana README'de listelendiği grup, hocalar.json "gruplar" içindeki adlardan biri. */
    public String grup = "";
    public String ofis = "";
    public String link = "";
    public boolean hoca_aktif_gorevde_mi = true;
    /** true: 👨‍🏫, false: 👩‍🏫, null: 🧑‍🏫. */
    public Boolean erkek_mi;
    /**
     * Hocanın verdiği derslerin adları. Hoca kartındaki ders sırası en son yıla göre azalandır;
     * aynı yıl bitenler bu listedeki sırayla dizilir. Araç README üretirken listeyi derslerdeki
     * akademisyen bilgisiyle eşitler (yeni dersler sona eklenir, kalmayanlar çıkar).
     */
    public List<String> dersler = new ArrayList<>();

    // --- Eski arayüz alanı, JSON'a yazılmaz.
    @JsonIgnore
    public List<String> bolumler = new ArrayList<>();

    @JsonIgnore
    public String emoji() {
        return erkek_mi == null ? "🧑‍🏫" : erkek_mi ? "👨‍🏫" : "👩‍🏫";
    }
}
