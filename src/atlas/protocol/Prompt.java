package atlas.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A reusable prompt template the model can request by name.
 *
 * <p>A prompt has a set of typed {@link PromptArgument}s and a
 * {@link PromptTemplate} that renders those arguments into one or more
 * messages. The server returns the rendered messages from a
 * {@code prompts/get} request.</p>
 */
public final class Prompt {

    private final String name;
    private final String description;
    private final List<PromptArgument> arguments;
    private final PromptTemplate template;

    public Prompt(String name, String description, List<PromptArgument> arguments,
                  PromptTemplate template) {
        this.name = name;
        this.description = description;
        this.arguments = arguments == null ? List.of() : arguments;
        this.template = template;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<PromptArgument> getArguments() {
        return arguments;
    }

    /** Render the prompt with the supplied arguments into protocol messages. */
    public List<Map<String, Object>> render(Map<String, Object> arguments) {
        return template.render(arguments == null ? Map.of() : arguments);
    }

    public Map<String, Object> toDefinition() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        List<Map<String, Object>> args = new ArrayList<>();
        for (PromptArgument a : arguments) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("name", a.name());
            am.put("description", a.description());
            am.put("required", a.required());
            args.add(am);
        }
        m.put("arguments", args);
        return m;
    }

    /** Contract for turning prompt arguments into chat messages. */
    @FunctionalInterface
    public interface PromptTemplate {
        List<Map<String, Object>> render(Map<String, Object> arguments);
    }

    /** A single named argument a prompt accepts. */
    public record PromptArgument(String name, String description, boolean required) {
    }
}
