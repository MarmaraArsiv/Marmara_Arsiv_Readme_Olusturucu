package edu.marmara.readme.engine.model;

/** Python: {"ad": str, "kisaltma": str} — bir dersin "dersi_veren_hocalar" listesindeki giriş. */
public class HocaKisaRef extends JsonNesnesi {
    public String ad = "";
    public String kisaltma = "";

    public HocaKisaRef() {
    }

    public HocaKisaRef(String ad, String kisaltma) {
        this.ad = ad;
        this.kisaltma = kisaltma;
    }
}
