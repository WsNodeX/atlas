package atlas.protocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny fluent builder for JSON Schema fragments used in tool {@code inputSchema}
 * blocks.
 *
 * <p>Hand writing nested schema maps is noisy and error prone. These helpers
 * produce the small, conventional subset the protocol expects
 * ({@code type}/{@code description}/{@code properties}/{@code required}) so a
 * tool definition reads almost like a declaration.</p>
 */
public final class Schemas {

    private Schemas() {
    }

    public static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("properties", properties);
        if (required != null && !required.isEmpty()) {
            m.put("required", required);
        }
        return m;
    }

    public static Map<String, Object> string(String description) {
        return primitive("string", description);
    }

    public static Map<String, Object> number(String description) {
        return primitive("number", description);
    }

    public static Map<String, Object> integer(String description) {
        return primitive("integer", description);
    }

    public static Map<String, Object> bool(String description) {
        return primitive("boolean", description);
    }

    private static Map<String, Object> primitive(String type, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        if (description != null) {
            m.put("description", description);
        }
        return m;
    }
}
