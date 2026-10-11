package edu.marmara.readme.engine.model;

import java.util.ArrayList;
import java.util.List;

/** donemler.json kök yapısı: 8 dönem ve seçmeli havuzları, README'deki sırayla. */
public class DonemlerBolumu extends JsonNesnesi {
    public List<Donem> donemler = new ArrayList<>();
    public List<Havuz> havuzlar = new ArrayList<>();
}
