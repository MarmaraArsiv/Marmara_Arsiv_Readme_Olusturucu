package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * repo_kullanimi.json kök yapısı: "Repo Kullanımı" ve "Kavramlar" bölümleri.
 * "aciklama"/"talimat"/"kavram" (tekil) alanları, listelerinin README'deki başlık metnidir.
 */
public class RepoKullanimiBolumu extends JsonNesnesi {
    public String baslik = "Repo Kullanımı";
    public String aciklama = "Açıklamalar";
    public List<String> aciklamalar = new ArrayList<>();
    public String talimat = "Talimatlar";
    public List<String> talimatlar = new ArrayList<>();
    public String kavram = "Kavramlar";
    /** Kavramlar listesinin üstündeki açıklama paragrafı (satır sonları korunur). */
    public String kavram_aciklamasi = "";
    /** README'deki sırayla. */
    public List<Kavram> kavramlar = new ArrayList<>();
}
