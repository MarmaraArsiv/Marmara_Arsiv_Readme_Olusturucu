package edu.marmara.readme.engine.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tek, paylaşılan Jackson ObjectMapper. Model sınıfları snake_case public alanlar kullanır
 * (JSON anahtarlarıyla birebir aynı isim), bu yüzden getter/setter gerekmez.
 *
 * Yazım biçimi Python'un json.dumps(..., ensure_ascii=False, indent=4) + "\n" çıktısıyla
 * birebir aynıdır ("anahtar": değer, boş liste [], Türkçe karakterler kaçışsız, sonda satır sonu).
 * GitHub'daki betikler de JSON'ları Python ile yazdığı için araç kaydettiğinde gereksiz fark oluşmaz.
 */
public final class JsonUtil {

    public static final ObjectMapper MAPPER = buildMapper();
    private static final DefaultPrettyPrinter PYTHON_BICIMI = pythonBicimi();

    private JsonUtil() {
    }

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static DefaultPrettyPrinter pythonBicimi() {
        Separators ayiraclar = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter(ayiraclar);
        DefaultIndenter girinti = new DefaultIndenter("    ", "\n");
        pp.indentObjectsWith(girinti);
        pp.indentArraysWith(girinti);
        return pp;
    }

    /** JSON dosyasını verilen tipe okur; dosya yoksa null döner. */
    public static <T> T read(Path path, Class<T> type) {
        if (!Files.exists(path)) {
            return null;
        }
        try {
            return MAPPER.readValue(Files.readString(path, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new RuntimeException("JSON okunamadı: " + path, e);
        }
    }

    /** Veriyi Python json.dumps(indent=4, ensure_ascii=False) biçiminde metne çevirir (sonda satır sonu ile). */
    public static String metin(Object data) {
        try {
            return MAPPER.writer(PYTHON_BICIMI).writeValueAsString(data) + "\n";
        } catch (IOException e) {
            throw new RuntimeException("JSON'a çevrilemedi", e);
        }
    }

    /** Veriyi JSON olarak UTF-8 ile yazar; üst dizinleri oluşturur. */
    public static void write(Path path, Object data) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, metin(data), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("JSON yazılamadı: " + path, e);
        }
    }
}
