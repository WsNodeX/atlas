package atlas.server;

import atlas.json.Json;
import atlas.protocol.Capabilities;
import atlas.protocol.Content;
import atlas.protocol.Messages;
import atlas.protocol.Prompt;
import atlas.protocol.Resource;
import atlas.protocol.Tool;
import atlas.protocol.ToolException;
import atlas.registry.Registry;
import atlas.transport.Transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dispatch engine that turns wire messages into registry calls.
 *
 * <p>{@code McpServer} owns the request loop: it reads newline-delimited
 * JSON-RPC messages from a {@link Transport}, routes each request to the right
 * handler, and writes the response back. Notifications (messages without an id,
 * such as {@code notifications/initialized}) are acknowledged silently, which
 * matches the protocol.</p>
 *
 * <p>Supported methods:
 * <ul>
 *   <li>{@code initialize} / {@code ping}</li>
 *   <li>{@code tools/list} / {@code tools/call}</li>
 *   <li>{@code resources/list} / {@code resources/read}</li>
 *   <li>{@code prompts/list} / {@code prompts/get}</li>
 * </ul>
 * </p>
 */
public final class McpServer {

    /** Protocol version this server speaks. */
    public static final String PROTOCOL_VERSION = "2024-11-05";

    private final Registry registry;
    private final String serverName;
    private final String serverVersion;

    public McpServer(Registry registry, String serverName, String serverVersion) {
        this.registry = registry;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    public Registry getRegistry() {
        return registry;
    }

    /**
     * Run the server loop until the transport signals end-of-input.
     *
     * @param transport the channel to read from / write to (stdio in production)
     */
    public void serve(Transport transport) throws IOException {
        try {
            String line;
            while ((line = transport.readMessage()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                handleLine(transport, line);
            }
        } finally {
            transport.close();
        }
    }

    /** Parse and dispatch a single line, writing any required response. */
    void handleLine(Transport transport, String line) throws IOException {
        Messages.Inbound inbound;
        try {
            inbound = Messages.parse(line);
        } catch (RuntimeException ex) {
            transport.writeMessage(Json.stringify(
                    Messages.error(null, Messages.ERROR_PARSE, "Malformed JSON: " + ex.getMessage())));
            return;
        }

        Map<String, Object> response = dispatch(inbound);
        if (response != null) {
            transport.writeMessage(Json.stringify(response));
        }
    }

    /**
     * Route one inbound message. Returns the response envelope for requests, or
     * {@code null} when no response is expected (notification / unhandled).
     */
    Map<String, Object> dispatch(Messages.Inbound inbound) {
        String method = inbound.method();
        Object id = inbound.id();

        if (!inbound.isRequest()) {
            // Notification: nothing to answer. Known lifecycle notifications are
            // simply consumed; unknown ones are ignored as well.
            return null;
        }

        try {
            return switch (method) {
                case "initialize" -> Messages.response(id, initializeResult());
                case "ping" -> Messages.response(id, new LinkedHashMap<>());
                case "tools/list" -> Messages.response(id, Map.of("tools", registry.listTools()));
                case "tools/call" -> Messages.response(id, handleToolCall(inbound.params()));
                case "resources/list" ->
                        Messages.response(id, Map.of("resources", registry.listResources()));
                case "resources/read" -> Messages.response(id, handleResourceRead(inbound.params()));
                case "prompts/list" -> Messages.response(id, Map.of("prompts", registry.listPrompts()));
                case "prompts/get" -> Messages.response(id, handlePromptGet(inbound.params()));
                default -> Messages.error(id, Messages.ERROR_METHOD_NOT_FOUND,
                        "Method not found: " + method);
            };
        } catch (ToolException ex) {
            return toolError(id, ex.getMessage());
        } catch (RuntimeException ex) {
            return Messages.error(id, Messages.ERROR_INTERNAL,
                    "Internal error: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------

    private Map<String, Object> initializeResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", Capabilities.serverCapabilities(
                registry.hasTools(), registry.hasResources(), registry.hasPrompts(), false));
        result.put("serverInfo", Capabilities.implementation(serverName, serverVersion));
        return result;
    }

    private Map<String, Object> handleToolCall(Map<String, Object> params) throws ToolException {
        String name = (String) params.get("name");
        if (name == null || name.isBlank()) {
            throw new ToolException("tool call missing 'name'");
        }
        Tool tool = registry.getTool(name);
        if (tool == null) {
            return toolErrorPayload("Unknown tool: " + name);
        }
        Object rawArgs = params.get("arguments");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = rawArgs instanceof Map ? (Map<String, Object>) rawArgs : Map.of();
        List<Content> contents = tool.call(arguments);
        return toolResultPayload(contents, false);
    }

    private Map<String, Object> handleResourceRead(Map<String, Object> params) throws ToolException {
        String uri = (String) params.get("uri");
        if (uri == null || uri.isBlank()) {
            throw new ToolException("resources/read missing 'uri'");
        }
        Resource resource = registry.getResource(uri);
        if (resource == null) {
            throw new ToolException("Unknown resource: " + uri);
        }
        Map<String, Object> contents = new LinkedHashMap<>();
        contents.put("uri", resource.getUri());
        contents.put("mimeType", resource.getMimeType());
        contents.put("text", resource.readText());
        return Map.of("contents", List.of(contents));
    }

    private Map<String, Object> handlePromptGet(Map<String, Object> params) throws ToolException {
        String name = (String) params.get("name");
        if (name == null || name.isBlank()) {
            throw new ToolException("prompts/get missing 'name'");
        }
        Prompt prompt = registry.getPrompt(name);
        if (prompt == null) {
            throw new ToolException("Unknown prompt: " + name);
        }
        Object rawArgs = params.get("arguments");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = rawArgs instanceof Map ? (Map<String, Object>) rawArgs : Map.of();
        List<Map<String, Object>> messages = prompt.render(arguments);
        return Map.of("messages", messages);
    }

    // ------------------------------------------------------------------
    // Response payload helpers
    // ------------------------------------------------------------------

    private Map<String, Object> toolResultPayload(List<Content> contents, boolean isError) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Content c : contents) {
            serialized.add(c.toMap());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", serialized);
        result.put("isError", isError);
        return result;
    }

    private Map<String, Object> toolErrorPayload(String message) {
        List<Content> contents = new ArrayList<>();
        contents.add(Content.text(message));
        return toolResultPayload(contents, true);
    }

    private Map<String, Object> toolError(Object id, String message) {
        return Messages.response(id, toolErrorPayload(message));
    }
}
