package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** Bir dersin bir akademik yılda (ör. "2025-2026") kimler tarafından yürütüldüğü. */
public class AkademisyenYili extends JsonNesnesi {
    public String yil = "";
    public List<DersHocasi> hocalar = new ArrayList<>();
}
