package atlas.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser and serializer.
 *
 * <p>It intentionally covers exactly the subset of JSON that the Model Context
 * Protocol needs: objects, arrays, strings, numbers, booleans and null. Keeping
 * it self-contained means {@code atlas} can run on a bare JDK 21 install with no
 * external jars on the classpath.</p>
 *
 * <p>Parsing returns plain {@link Map}/{@link List} structures (using insertion
 * ordered maps so key order is preserved when echoing payloads back), and
 * {@link #stringify(Object)} turns those structures back into compact JSON.</p>
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    public static Object parse(String text) {
        if (text == null) {
            throw new JsonException("Cannot parse null");
        }
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.parseValue();
        p.skipWhitespace();
        if (!p.isAtEnd()) {
            throw new JsonException("Trailing characters at position " + p.pos);
        }
        return value;
    }

    /** Parse and cast the root to a map (object). */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new JsonException("Expected a JSON object at root");
        }
        return (Map<String, Object>) v;
    }

    // ------------------------------------------------------------------
    // Serializing
    // ------------------------------------------------------------------

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    /** Serialize with 2-space indentation for human readable output. */
    public static String pretty(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value, 0);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value.toString());
        } else if (value instanceof Map) {
            writeObject(sb, (Map<?, ?>) value);
        } else if (value instanceof List) {
            writeArray(sb, (List<?>) value);
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            writeValue(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static void writeValue(StringBuilder sb, Object value, int indent) {
        if (value instanceof Map || value instanceof List) {
            writePretty(sb, value, indent);
        } else {
            writeValue(sb, value);
        }
    }

    private static void writePretty(StringBuilder sb, Object value, int indent) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                indent(sb, indent + 1);
                writeString(sb, String.valueOf(e.getKey()));
                sb.append(": ");
                writeValue(sb, e.getValue(), indent + 1);
            }
            sb.append('\n');
            indent(sb, indent);
            sb.append('}');
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                indent(sb, indent + 1);
                writeValue(sb, item, indent + 1);
            }
            sb.append('\n');
            indent(sb, indent);
            sb.append(']');
        } else {
            writeValue(sb, value);
        }
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
    }

    // ------------------------------------------------------------------
    // Parser implementation
    // ------------------------------------------------------------------

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
            this.pos = 0;
        }

        boolean isAtEnd() {
            return pos >= s.length();
        }

        void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) {
                pos++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw new JsonException("Unexpected end of input");
            }
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            pos++; // consume '{'
            skipWhitespace();
            if (peek() == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw new JsonException("Expected key string at position " + pos);
                }
                String key = parseString();
                skipWhitespace();
                if (peek() != ':') {
                    throw new JsonException("Expected ':' at position " + pos);
                }
                pos++; // consume ':'
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                char n = peek();
                if (n == ',') {
                    pos++;
                } else if (n == '}') {
                    pos++;
                    break;
                } else {
                    throw new JsonException("Expected ',' or '}' at position " + pos);
                }
            }
            return map;
        }

        List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // consume '['
            skipWhitespace();
            if (peek() == ']') {
                pos++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char n = peek();
                if (n == ',') {
                    pos++;
                } else if (n == ']') {
                    pos++;
                    break;
                } else {
                    throw new JsonException("Expected ',' or ']' at position " + pos);
                }
            }
            return list;
        }

        String parseString() {
            pos++; // consume opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '"') {
                    pos++;
                    return sb.toString();
                }
                if (c == '\\') {
                    pos++;
                    if (pos >= s.length()) {
                        throw new JsonException("Unterminated escape");
                    }
                    char e = s.charAt(pos);
                    switch (e) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 >= s.length()) {
                                throw new JsonException("Invalid unicode escape");
                            }
                            String hex = s.substring(pos + 1, pos + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                        }
                        default -> sb.append(e);
                    }
                    pos++;
                } else {
                    sb.append(c);
                    pos++;
                }
            }
            throw new JsonException("Unterminated string");
        }

        Object parseNumber() {
            int start = pos;
            boolean isDouble = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '-' || c == '+' || (c >= '0' && c <= '9')) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E') {
                    isDouble = true;
                    pos++;
                } else {
                    break;
                }
            }
            String num = s.substring(start, pos);
            try {
                if (isDouble) {
                    return Double.parseDouble(num);
                }
                long l = Long.parseLong(num);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (NumberFormatException ex) {
                throw new JsonException("Invalid number: " + num);
            }
        }

        Boolean parseBoolean() {
            if (s.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        Object parseNull() {
            if (s.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonException("Invalid literal at position " + pos);
        }

        char peek() {
            if (pos >= s.length()) {
                throw new JsonException("Unexpected end of input");
            }
            return s.charAt(pos);
        }
    }

    /** Thrown when JSON cannot be parsed or serialized. */
    public static final class JsonException extends RuntimeException {
        public JsonException(String message) {
            super(message);
        }
    }
}
