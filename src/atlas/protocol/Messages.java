package atlas.protocol;

import atlas.json.Json;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-RPC 2.0 message shapes used by the protocol.
 *
 * <p>Every exchange on the wire is either a {@code Request} (expects a
 * response), a {@code Notification} (fire-and-forget, no id, no response) or a
 * {@code Response} (carries a result or an error for a previous request id).
 * This class provides small typed wrappers plus (de)serialization helpers so
 * the server never has to hand-build the envelope.</p>
 */
public final class Messages {

    public static final String JSONRPC = "2.0";
    public static final int ERROR_METHOD_NOT_FOUND = -32601;
    public static final int ERROR_INVALID_PARAMS = -32602;
    public static final int ERROR_INTERNAL = -32603;
    public static final int ERROR_PARSE = -32700;

    private Messages() {
    }

    /** Parse an incoming wire line into a request or notification. */
    public static Inbound parse(String line) {
        Map<String, Object> raw = Json.parseObject(line);
        Object id = raw.get("id");
        String method = (String) raw.get("method");
        Object params = raw.getOrDefault("params", Map.of());
        boolean isRequest = raw.containsKey("id");
        return new Inbound(id, method, params, isRequest);
    }

    /** Build a successful response envelope. */
    public static Map<String, Object> response(Object id, Object result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", JSONRPC);
        m.put("id", id);
        m.put("result", result);
        return m;
    }

    /** Build an error response envelope. */
    public static Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", JSONRPC);
        m.put("id", id);
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("code", code);
        err.put("message", message);
        m.put("error", err);
        return m;
    }

    /** Build a notification envelope (no id). */
    public static Map<String, Object> notification(String method, Object params) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", JSONRPC);
        m.put("method", method);
        if (params != null) {
            m.put("params", params);
        }
        return m;
    }

    /** A parsed inbound message. */
    public static final class Inbound {
        private final Object id;
        private final String method;
        private final Object params;
        private final boolean request;

        Inbound(Object id, String method, Object params, boolean request) {
            this.id = id;
            this.method = method;
            this.params = params;
            this.request = request;
        }

        public Object id() {
            return id;
        }

        public String method() {
            return method;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> params() {
            return params instanceof Map ? (Map<String, Object>) params : Map.of();
        }

        public boolean isRequest() {
            return request;
        }
    }
}
