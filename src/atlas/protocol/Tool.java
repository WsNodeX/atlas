package atlas.protocol;

import java.util.List;
import java.util.Map;

/**
 * A callable tool exposed to a language model.
 *
 * <p>A tool bundles three things the protocol needs:
 * <ol>
 *   <li>a unique {@code name} the model refers to,</li>
 *   <li>a human readable {@code description} and a JSON {@code inputSchema}
 *       so the model knows what arguments to pass,</li>
 *   <li>a {@link ToolHandler} that turns arguments into {@link Content}.</li>
 * </ol>
 *
 * <p>Handlers receive already-parsed arguments and should throw
 * {@link ToolException} to signal a user-facing failure (which the server
 * reports back as an {@code isError: true} tool result rather than a transport
 * error).</p>
 */
public final class Tool {

    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;
    private final ToolHandler handler;

    public Tool(String name, String description, Map<String, Object> inputSchema, ToolHandler handler) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema == null ? Map.of() : inputSchema;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    /** Execute the tool with the supplied arguments. */
    public List<Content> call(Map<String, Object> arguments) throws ToolException {
        return handler.apply(arguments == null ? Map.of() : arguments);
    }

    /** The {@code tools/list} entry describing this tool. */
    public Map<String, Object> toDefinition() {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("inputSchema", inputSchema);
        return m;
    }

    /**
     * Functional contract for a tool's execution. Implementations may throw
     * {@link ToolException} to report a recoverable, user-facing failure.
     */
    @FunctionalInterface
    public interface ToolHandler {
        List<Content> apply(Map<String, Object> arguments) throws ToolException;
    }
}
