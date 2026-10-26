package atlas.protocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A URI-addressable piece of read-only content (file, config, web page, ...).
 *
 * <p>Resources let a server expose data the model can pull on demand. The text
 * is produced lazily through a {@link Supplier} so expensive reads only happen
 * when a {@code resources/read} request actually arrives.</p>
 */
public final class Resource {

    private final String uri;
    private final String name;
    private final String description;
    private final String mimeType;
    private final Supplier<String> textSupplier;

    public Resource(String uri, String name, String description, String mimeType,
                    Supplier<String> textSupplier) {
        this.uri = uri;
        this.name = name;
        this.description = description;
        this.mimeType = mimeType == null ? "text/plain" : mimeType;
        this.textSupplier = textSupplier;
    }

    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String readText() {
        return textSupplier.get();
    }

    /** Entry for {@code resources/list}. */
    public Map<String, Object> toDefinition() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uri", uri);
        m.put("name", name);
        m.put("description", description);
        m.put("mimeType", mimeType);
        return m;
    }
}
