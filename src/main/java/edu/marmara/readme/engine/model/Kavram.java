package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** Python: {"kavram": str, "aciklamalar": [str, ...]} — Repo Kullanımı > Kavramlar listesindeki bir öğe. */
public class Kavram extends JsonNesnesi {
    public String kavram = "";
    public List<String> aciklamalar = new ArrayList<>();
}
