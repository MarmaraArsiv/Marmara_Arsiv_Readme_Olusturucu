package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Python: {"oneri_sahibi": str, "oneriler": [str, ...]} — ders.derse_dair_oneriler listesinin
 * bir öğesi. Her "oneri" serbest metindir ve içinde markdown linki olabilir (link göreceli
 * hale getirme burada uygulanır, bkz. Yardimci.kaynakLinkleriniGoreceliYap).
 */
public class OneriGrubu extends JsonNesnesi {
    public String oneri_sahibi = "";
    public List<String> oneriler = new ArrayList<>();
}
