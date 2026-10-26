package atlas;

import atlas.examples.Demo;
import atlas.json.Json;
import atlas.protocol.Messages;
import atlas.server.McpServer;
import atlas.transport.InMemoryTransport;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerDispatchTest {

    private static String request(Object id, String method, Map<String, Object> params) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", Messages.JSONRPC);
        m.put("id", id);
        m.put("method", method);
        m.put("params", params);
        return Json.stringify(m);
    }

    /** Feed a sequence of request lines, run the server, return parsed responses. */
    private static List<Map<String, Object>> run(McpServer server, String... lines) throws Exception {
        InMemoryTransport t = new InMemoryTransport();
        for (String l : lines) {
            t.feed(l);
        }
        t.end();
        server.serve(t);
        return t.getOutgoing().stream().map(Json::parseObject).toList();
    }

    private static McpServer server() {
        return Demo.demoServer();
    }

    @Test
    void initializeReportsCapabilitiesAndServerInfo() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(1, "initialize", Map.of("protocolVersion", "2024-11-05")));
        Map<String, Object> result = resultOf(out.get(0));
        assertEquals("2024-11-05", result.get("protocolVersion"));
        @SuppressWarnings("unchecked")
        Map<String, Object> caps = (Map<String, Object>) result.get("capabilities");
        assertTrue(caps.containsKey("tools"));
        assertTrue(caps.containsKey("resources"));
        assertTrue(caps.containsKey("prompts"));
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) result.get("serverInfo");
        assertEquals("atlas-demo", info.get("name"));
    }

    @Test
    void toolsListReturnsRegisteredTools() throws Exception {
        List<Map<String, Object>> out = run(server(), request(2, "tools/list", Map.of()));
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) out.get(0).get("result");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        assertTrue(tools.size() >= 4);
        boolean hasCalculator = tools.stream().anyMatch(t -> "calculator".equals(t.get("name")));
        assertTrue(hasCalculator);
    }

    @Test
    void toolCallPerformsArithmetic() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(3, "tools/call", Map.of("name", "calculator",
                        "arguments", Map.of("expression", "(2+3)*4/2"))));
        Map<String, Object> result = resultOf(out.get(0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertEquals("(2+3)*4/2 = 10", content.get(0).get("text"));
        assertEquals(Boolean.FALSE, result.get("isError"));
    }

    @Test
    void toolCallEchoesText() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(4, "tools/call", Map.of("name", "echo",
                        "arguments", Map.of("text", "hello atlas"))));
        Map<String, Object> result = resultOf(out.get(0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertEquals("hello atlas", content.get(0).get("text"));
    }

    @Test
    void toolCallReportsErrorForDivisionByZero() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(5, "tools/call", Map.of("name", "calculator",
                        "arguments", Map.of("expression", "10/0"))));
        Map<String, Object> result = resultOf(out.get(0));
        assertEquals(Boolean.TRUE, result.get("isError"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        assertTrue(((String) content.get(0).get("text")).toLowerCase().contains("zero"));
    }

    @Test
    void toolCallUnknownToolIsError() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(6, "tools/call", Map.of("name", "nope",
                        "arguments", Map.of())));
        Map<String, Object> result = resultOf(out.get(0));
        assertEquals(Boolean.TRUE, result.get("isError"));
    }

    @Test
    void unknownMethodReturnsMethodNotFound() throws Exception {
        List<Map<String, Object>> out = run(server(), request(7, "foo/bar", Map.of()));
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) out.get(0).get("error");
        assertEquals(Messages.ERROR_METHOD_NOT_FOUND, error.get("code"));
    }

    @Test
    void pingReturnsEmptyResult() throws Exception {
        List<Map<String, Object>> out = run(server(), request(8, "ping", Map.of()));
        Map<String, Object> result = resultOf(out.get(0));
        assertTrue(result.isEmpty());
    }

    @Test
    void resourceReadReturnsText() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(9, "resources/read", Map.of("uri", "atlas://readme")));
        Map<String, Object> result = resultOf(out.get(0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> contents = (List<Map<String, Object>>) result.get("contents");
        assertTrue(((String) contents.get(0).get("text")).contains("Atlas"));
    }

    @Test
    void promptGetRendersMessages() throws Exception {
        List<Map<String, Object>> out = run(server(),
                request(10, "prompts/get", Map.of("name", "explain",
                        "arguments", Map.of("topic", "vector databases"))));
        Map<String, Object> result = resultOf(out.get(0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) result.get("messages");
        assertEquals(1, messages.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> content = (Map<String, Object>) messages.get(0).get("content");
        assertTrue(((String) content.get("text")).contains("vector databases"));
    }

    @Test
    void notificationProducesNoResponse() throws Exception {
        InMemoryTransport t = new InMemoryTransport();
        t.feed(request(11, "initialize", Map.of()));
        t.feed("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\",\"params\":{}}");
        t.end();
        server().serve(t);
        // Only the initialize response should be present.
        assertEquals(1, t.getOutgoing().size());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resultOf(Map<String, Object> response) {
        Object r = response.get("result");
        assertInstanceOf(Map.class, r);
        return (Map<String, Object>) r;
    }
}
