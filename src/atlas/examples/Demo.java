package atlas.examples;

import atlas.examples.tools.CalculatorTool;
import atlas.examples.tools.EchoTool;
import atlas.examples.tools.SystemInfoTool;
import atlas.examples.tools.TimeTool;
import atlas.json.Json;
import atlas.protocol.Messages;
import atlas.protocol.Prompt;
import atlas.protocol.Resource;
import atlas.registry.Registry;
import atlas.server.McpServer;
import atlas.transport.InMemoryTransport;
import atlas.transport.StdioTransport;
import atlas.transport.Transport;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference server showing how to assemble an Atlas MCP server.
 *
 * <p>Run with no arguments to watch a scripted conversation play out over an
 * in-memory transport (great for understanding the wire protocol), or pass
 * {@code --stdio} to launch a real stdio server that any MCP-capable client can
 * connect to.</p>
 *
 * <pre>
 *   java atlas.examples.Demo            # scripted demo
 *   java atlas.examples.Demo --stdio    # production stdio server
 * </pre>
 */
public final class Demo {

    private static final String SERVER_NAME = "atlas-demo";
    private static final String SERVER_VERSION = "1.0.0";

    private Demo() {
    }

    public static void main(String[] args) throws IOException {
        Registry registry = buildRegistry();
        McpServer server = new McpServer(registry, SERVER_NAME, SERVER_VERSION);

        if (args.length > 0 && args[0].equals("--stdio")) {
            server.serve(StdioTransport.system());
            return;
        }
        runScriptedDemo(server);
    }

    /** Build a registry populated with the bundled example capabilities. */
    public static Registry buildRegistry() {
        Registry registry = new Registry();
        registry.register(CalculatorTool.create());
        registry.register(EchoTool.create());
        registry.register(TimeTool.create());
        registry.register(SystemInfoTool.create());
        registry.register(readmeResource());
        registry.register(explainPrompt());
        return registry;
    }

    private static Resource readmeResource() {
        return new Resource("atlas://readme", "Project README",
                "Short description of the Atlas demo server", "text/markdown",
                () -> "# Atlas demo server\n"
                        + "A zero-dependency Java framework for building MCP servers.\n"
                        + "Tools: calculator, echo, current_time, system_info.");
    }

    private static Prompt explainPrompt() {
        List<Prompt.PromptArgument> args = List.of(
                new Prompt.PromptArgument("topic", "The subject to explain", true));
        return new Prompt("explain", "Ask the model to explain a topic simply",
                args, arguments -> {
                    String topic = String.valueOf(arguments.getOrDefault("topic", "this"));
                    Map<String, Object> content = new LinkedHashMap<>();
                    content.put("type", "text");
                    content.put("text", "Explain \"" + topic
                            + "\" in three bullet points a beginner would understand.");
                    Map<String, Object> message = new LinkedHashMap<>();
                    message.put("role", "user");
                    message.put("content", content);
                    return List.of(message);
                });
    }

    // ------------------------------------------------------------------
    // Scripted demo over an in-memory transport
    // ------------------------------------------------------------------

    private static void runScriptedDemo(McpServer server) throws IOException {
        InMemoryTransport transport = new InMemoryTransport();
        transport.feed(request(1, "initialize", Map.of(
                "protocolVersion", "2024-11-05",
                "clientInfo", Map.of("name", "demo-client", "version", "0.1"))));
        transport.feed(notification("notifications/initialized", Map.of()));
        transport.feed(request(2, "tools/list", Map.of()));
        transport.feed(request(3, "tools/call", Map.of(
                "name", "calculator", "arguments", Map.of("expression", "(2+3)*4/2"))));
        transport.feed(request(4, "tools/call", Map.of(
                "name", "echo", "arguments", Map.of("text", "hello atlas"))));
        transport.feed(request(5, "tools/call", Map.of(
                "name", "current_time", "arguments", Map.of("zone", "Asia/Shanghai"))));
        transport.feed(request(6, "resources/read", Map.of("uri", "atlas://readme")));
        transport.feed(request(7, "prompts/get", Map.of(
                "name", "explain", "arguments", Map.of("topic", "vector databases"))));
        transport.feed(request(8, "ping", Map.of()));
        transport.feed(request(9, "tools/call", Map.of(
                "name", "calculator", "arguments", Map.of("expression", "10/0"))));
        transport.end();

        server.serve(transport);

        System.out.println("=== Atlas scripted demo transcript ===");
        for (String line : transport.getOutgoing()) {
            Object parsed = Json.parse(line);
            System.out.println(Json.pretty(parsed));
            System.out.println("---");
        }
    }

    private static String request(Object id, String method, Map<String, Object> params) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", Messages.JSONRPC);
        m.put("id", id);
        m.put("method", method);
        m.put("params", params);
        return Json.stringify(m);
    }

    private static String notification(String method, Map<String, Object> params) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", Messages.JSONRPC);
        m.put("method", method);
        m.put("params", params);
        return Json.stringify(m);
    }

    /** Exposed so unit tests can build a ready-to-use server. */
    public static McpServer demoServer() {
        return new McpServer(buildRegistry(), SERVER_NAME, SERVER_VERSION);
    }
}
