package edu.marmara.readme.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

/** konfigurasyon.json kök yapısı. */
public class Konfigurasyon extends JsonNesnesi {
    public String github_url = "";
    public String hoca_yorumlama = "";
    public String hoca_oylama = "";
    public String ders_yorumlama = "";
    public String ders_oylama = "";
    public String ders_oylama_csv = "";
    public String ders_yorumlama_csv = "";
    public String hoca_oylama_csv = "";
    public String hoca_yorumlama_csv = "";
    /** JSON klasörüne göre arşiv reposunun kökü. */
    public String dokumanlar_repo_yolu = "..";
    public String cikmislar = "";
    /**
     * Google Form ayrıntıları (link, soru kimlikleri, CSV adresleri). GitHub'daki form_linkleri.py
     * betiği de bunu okur; araç yalnızca link ve "…_alani" kimliklerini kullanır, gerisini aynen korur.
     */
    public Map<String, Object> formlar = new LinkedHashMap<>();
    /** Müfredat seçenekleri ve eski müfredat notu kuralı (bölüme göre değişir). */
    public MufredatAyari mufredat = new MufredatAyari();

    /** Formun linki ve gereken soru kimliği girilmişse true; formları henüz kurulmamış bölümlerde false. */
    @JsonIgnore
    public boolean formHazir(String form, String alan) {
        return !formAlani(form, "link").isBlank() && !formAlani(form, alan).isBlank();
    }

    /** formlar.<form>.<alan> değeri; yoksa boş metin. */
    @JsonIgnore
    public String formAlani(String form, String alan) {
        Object f = formlar.get(form);
        if (f instanceof Map<?, ?> m && m.get(alan) != null) {
            return m.get(alan).toString();
        }
        return "";
    }
}
