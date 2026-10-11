package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** hocalar.json kök yapısı. Grupların sırası README'deki sıradır; grup içindeki hocalar ada göre dizilir. */
public class HocalarBolumu extends JsonNesnesi {
    public String bolum_adi = "Hocalar";
    public String bolum_aciklamasi = "";
    public List<HocaGrubu> gruplar = new ArrayList<>();
    public List<Hoca> hocalar = new ArrayList<>();
}
