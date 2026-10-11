package edu.marmara.readme.engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Bir akademik yılda dersi yürüten hoca. "not" isteğe bağlı kısa açıklamadır
 * (ör. "lab", "atölye"); README'de adın yanında parantez içinde gösterilir.
 */
public class DersHocasi extends JsonNesnesi {
    public String ad = "";
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String not = "";

    public DersHocasi() {
    }

    public DersHocasi(String ad, String not) {
        this.ad = ad;
        this.not = not == null ? "" : not;
    }
}
