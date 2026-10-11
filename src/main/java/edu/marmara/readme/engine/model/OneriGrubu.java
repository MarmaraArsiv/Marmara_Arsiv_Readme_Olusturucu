package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bir dersin "Derse Dair Öneriler" bölümünde, öneri sahibine göre gruplanmış öneriler.
 * Her öneri serbest metindir (markdown link içerebilir) ve README'ye olduğu gibi yazılır.
 */
public class OneriGrubu extends JsonNesnesi {
    public String oneri_sahibi = "";
    public List<String> oneriler = new ArrayList<>();
}
