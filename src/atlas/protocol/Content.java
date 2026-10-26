package atlas.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A piece of content returned by a tool, resource or prompt.
 *
 * <p>The Model Context Protocol distinguishes {@code text}, {@code image} and
 * {@code resource} content. This implementation ships a focused, fully working
 * {@code text} variant (the most common case for tool output) and is structured
 * so additional kinds can be added without touching the dispatch layer.</p>
 */
public final class Content {

    public static final String TYPE_TEXT = "text";

    private final String type;
    private final String text;

    private Content(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public static Content text(String text) {
        return new Content(TYPE_TEXT, text);
    }

    public String getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    /** Serialize to the JSON object the protocol expects. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        if (text != null) {
            m.put("text", text);
        }
        return m;
    }
}
