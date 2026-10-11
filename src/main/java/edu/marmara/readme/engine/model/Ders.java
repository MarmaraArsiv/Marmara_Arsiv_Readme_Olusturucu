package edu.marmara.readme.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * dersler.json içindeki "dersler" dizisinin bir öğesi. Dizideki sıra, dersin ana README'de
 * ve dönem README'sinde göründüğü sıradır.
 */
public class Ders extends JsonNesnesi {
    public String ad = "";
    /** Repo köküne göre ders klasörü, ör. "2-2/Elektronik Devrelere Giriş" ya da "Mesleki Seçmeli/Bulut Bilişim". */
    public String klasor = "";
    /** 1-4 arası sınıf; seçmeli havuz derslerinde 0. */
    public int yil;
    /** "Güz" ya da "Bahar"; seçmeli havuz derslerinde boş. */
    public String donem = "";
    /** README'deki "Ders Tipi" değeri, ör. "Zorunlu" ya da "Seçmeli". */
    public String tip = "";
    /** "Her İki Müfredat", "2026 Öncesi", "2026 Sonrası" ya da "2026 Öncesi (Zorunlu)" gibi. */
    public String mufredat = "";
    /** Müfredat satırının altındaki ℹ️ notu; boşsa yazılmaz. */
    public String mufredat_notu = "";
    /** Akademik yıla göre dersi yürüten hocalar, yeni yıl üstte. */
    public List<AkademisyenYili> akademisyenler = new ArrayList<>();
    public List<OneriGrubu> derse_dair_oneriler = new ArrayList<>();

    // --- Eski arayüz alanları: arayüz yeni yapıya geçene kadar derlensin diye duruyor, JSON'a yazılmaz.
    @JsonIgnore
    public boolean guncel_mi = true;
    @JsonIgnore
    public List<HocaKisaRef> dersi_veren_hocalar = new ArrayList<>();
    @JsonIgnore
    public List<String> faydali_olabilecek_kaynaklar = new ArrayList<>();
    @JsonIgnore
    public List<String> mufredatlar = new ArrayList<>();

    /** Klasör bir dönem klasöründeyse (1-1 … 4-2) o dönemin klasör adı, seçmeli havuzundaysa havuz adı. */
    @JsonIgnore
    public String ustKlasor() {
        int i = klasor.indexOf('/');
        return i < 0 ? klasor : klasor.substring(0, i);
    }

    /** Klasörün son parçası (README'deki ders adıyla genelde aynıdır). */
    @JsonIgnore
    public String klasorAdi() {
        int i = klasor.indexOf('/');
        return i < 0 ? klasor : klasor.substring(i + 1);
    }
}
