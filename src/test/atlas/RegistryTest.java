package atlas;

import atlas.protocol.Prompt;
import atlas.protocol.Resource;
import atlas.protocol.Tool;
import atlas.registry.Registry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryTest {

    private static Tool dummyTool() {
        return new Tool("t", "desc", Map.of(), args -> List.of());
    }

    @Test
    void registersAndLooksUpTool() {
        Registry r = new Registry();
        assertFalse(r.hasTools());
        r.register(dummyTool());
        assertTrue(r.hasTools());
        assertEquals("t", r.getTool("t").getName());
        assertNull(r.getTool("missing"));
    }

    @Test
    void listsToolDefinitionsInOrder() {
        Registry r = new Registry();
        r.register(new Tool("alpha", "a", Map.of(), args -> List.of()));
        r.register(new Tool("beta", "b", Map.of(), args -> List.of()));
        List<Map<String, Object>> defs = r.listTools();
        assertEquals(2, defs.size());
        assertEquals("alpha", defs.get(0).get("name"));
        assertEquals("beta", defs.get(1).get("name"));
    }

    @Test
    void registersResourceAndPrompt() {
        Registry r = new Registry();
        r.register(new Resource("uri:x", "n", "d", "text/plain", () -> "body"));
        r.register(new Prompt("p", "pd", List.of(), a -> List.of()));
        assertTrue(r.hasResources());
        assertTrue(r.hasPrompts());
        assertEquals("body", r.getResource("uri:x").readText());
        assertNotNull(r.getPrompt("p"));
    }
}
