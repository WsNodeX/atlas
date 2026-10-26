package atlas;

import atlas.json.Json;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonTest {

    @Test
    void parsesFlatObject() {
        Map<String, Object> m = Json.parseObject("{\"name\":\"atlas\",\"v\":3}");
        assertEquals("atlas", m.get("name"));
        assertEquals(3, m.get("v"));
    }

    @Test
    void parsesNestedArrayAndObject() {
        Map<String, Object> m = Json.parseObject("{\"a\":[1,2,{\"b\":true}],\"c\":null}");
        List<?> a = (List<?>) m.get("a");
        assertEquals(3, a.size());
        Map<?, ?> inner = (Map<?, ?>) a.get(2);
        assertEquals(Boolean.TRUE, inner.get("b"));
        assertTrue(m.get("c") == null);
    }

    @Test
    void parsesNumberTypes() {
        Map<String, Object> m = Json.parseObject("{\"i\":42,\"l\":9000000000,\"d\":3.14}");
        assertEquals(42, m.get("i"));
        assertEquals(9000000000L, m.get("l"));
        assertEquals(3.14, (Double) m.get("d"), 1e-9);
    }

    @Test
    void parsesEscapes() {
        Map<String, Object> m = Json.parseObject("{\"s\":\"line\\n\\ttab\\u0041\"}");
        assertEquals("line\n\ttabA", m.get("s"));
    }

    @Test
    void roundTripsThroughStringify() {
        Map<String, Object> original = Json.parseObject("{\"x\":1,\"y\":[true,null,\"z\"]}");
        String serialized = Json.stringify(original);
        Map<String, Object> reparsed = Json.parseObject(serialized);
        assertEquals(original, reparsed);
    }

    @Test
    void detectsTrailingCharacters() {
        assertThrows(Json.JsonException.class, () -> Json.parse("{} extra"));
    }

    @Test
    void detectsUnterminatedString() {
        assertThrows(Json.JsonException.class, () -> Json.parse("\"oops"));
    }

    @Test
    void prettyIndents() {
        Map<String, Object> m = Json.parseObject("{\"a\":1}");
        String pretty = Json.pretty(m);
        assertInstanceOf(String.class, pretty);
        assertTrue(pretty.contains("\n"));
    }
}
