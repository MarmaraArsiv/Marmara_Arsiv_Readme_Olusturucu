package edu.marmara.readme.engine.model;

/** Seçmeli ders havuzu (ör. Mesleki Seçmeli): repo kökünde kendi klasörü ve README'si olan ders grubu. */
public class Havuz extends JsonNesnesi {
    public String ad = "";
    public String klasor = "";
    /** Havuz README'sinde başlığın altındaki açıklama. */
    public String aciklama = "";
}
