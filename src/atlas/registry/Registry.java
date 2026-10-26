package atlas.registry;

import atlas.protocol.Prompt;
import atlas.protocol.Resource;
import atlas.protocol.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central catalogue of everything a server exposes.
 *
 * <p>The {@code Registry} is the single source of truth the dispatch loop
 * queries. Tools, resources and prompts are registered up front (typically in
 * a {@code main} method or a bootstrap class) and then listed or looked up by
 * name as requests arrive. Insertion order is preserved so {@code tools/list}
 * returns a stable ordering.</p>
 */
public final class Registry {

    private final Map<String, Tool> tools = new LinkedHashMap<>();
    private final Map<String, Resource> resources = new LinkedHashMap<>();
    private final Map<String, Prompt> prompts = new LinkedHashMap<>();

    public Registry register(Tool tool) {
        tools.put(tool.getName(), tool);
        return this;
    }

    public Registry register(Resource resource) {
        resources.put(resource.getUri(), resource);
        return this;
    }

    public Registry register(Prompt prompt) {
        prompts.put(prompt.getName(), prompt);
        return this;
    }

    public Tool getTool(String name) {
        return tools.get(name);
    }

    public Resource getResource(String uri) {
        return resources.get(uri);
    }

    public Prompt getPrompt(String name) {
        return prompts.get(name);
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    public boolean hasResources() {
        return !resources.isEmpty();
    }

    public boolean hasPrompts() {
        return !prompts.isEmpty();
    }

    public List<Map<String, Object>> listTools() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tool t : tools.values()) {
            out.add(t.toDefinition());
        }
        return out;
    }

    public List<Map<String, Object>> listResources() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Resource r : resources.values()) {
            out.add(r.toDefinition());
        }
        return out;
    }

    public List<Map<String, Object>> listPrompts() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Prompt p : prompts.values()) {
            out.add(p.toDefinition());
        }
        return out;
    }
}
