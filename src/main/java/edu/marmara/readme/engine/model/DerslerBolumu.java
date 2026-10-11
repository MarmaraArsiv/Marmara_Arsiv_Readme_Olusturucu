package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** dersler.json kök yapısı. */
public class DerslerBolumu extends JsonNesnesi {
    public String bolum_adi = "Dersler";
    public String bolum_aciklamasi = "";
    public String secmeli_bolum_adi = "Seçmeli Dersler";
    public String secmeli_bolum_aciklamasi = "";
    /** Her ders README'sinin sonundaki "🤝 Katkıda Bulun" paragrafı. */
    public String katkida_bulun_metni = "";
    public List<Ders> dersler = new ArrayList<>();
}
