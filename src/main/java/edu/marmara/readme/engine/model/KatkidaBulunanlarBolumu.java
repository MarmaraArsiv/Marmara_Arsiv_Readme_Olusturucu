package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** katkida_bulunanlar.json kök yapısı. */
public class KatkidaBulunanlarBolumu extends JsonNesnesi {
    public String bolum_adi = "Katkıda Bulunanlar";
    public String bolum_aciklamasi = "";
    public List<KatkidaBulunan> katkida_bulunanlar = new ArrayList<>();
}
