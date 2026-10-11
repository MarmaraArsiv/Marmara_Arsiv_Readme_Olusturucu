package edu.marmara.readme.engine.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tüm JSON modellerinin tabanı: modelde karşılığı olmayan alanları saklar ve dosya
 * geri yazılırken aynen korur. Böylece araç, tanımadığı bir alanı (ör. konfigurasyon.json
 * içine GitHub betiklerinin eklediği bir anahtar) arayüzden kaydederken silmez.
 */
public abstract class JsonNesnesi {

    @JsonIgnore
    private final Map<String, Object> ekstra = new LinkedHashMap<>();

    @JsonAnySetter
    public void ekstraAyarla(String ad, Object deger) {
        ekstra.put(ad, deger);
    }

    @JsonAnyGetter
    public Map<String, Object> ekstraAlanlar() {
        return ekstra;
    }
}
