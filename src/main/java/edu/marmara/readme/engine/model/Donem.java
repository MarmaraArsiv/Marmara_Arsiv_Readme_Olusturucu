package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** donemler.json içindeki "donemler" dizisinin bir öğesi (ör. 1-1: 1. Sınıf Güz Dönemi). */
public class Donem extends JsonNesnesi {
    public String donem_adi = "";
    /** Repo kökündeki dönem klasörü, ör. "1-1". */
    public String klasor = "";
    public int yil;
    public String donem = "";
    public List<String> genel_tavsiyeler = new ArrayList<>();
}
