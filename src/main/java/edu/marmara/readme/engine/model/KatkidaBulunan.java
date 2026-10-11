package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** katkida_bulunanlar.json içindeki bir kişi. */
public class KatkidaBulunan extends JsonNesnesi {
    public String ad = "";
    public String github_link = "";
    public String katkida_bulunma_orani = "";
    public List<IletisimBilgisi> iletisim_bilgileri = new ArrayList<>();
}
