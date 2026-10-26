package atlas.examples.tools;

import atlas.protocol.Content;
import atlas.protocol.Schemas;
import atlas.protocol.Tool;

import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free arithmetic calculator exposed as an MCP tool.
 *
 * <p>Rather than shelling out to {@code javax.script} (which pulls in a
 * scripting engine and is a classic injection foot-gun), the tool runs a tiny
 * recursive-descent evaluator that understands the four basic operators,
 * modulo, parentheses and unary minus. Anything outside that grammar is
 * rejected with a clear message instead of being executed.</p>
 */
public final class CalculatorTool {

    private CalculatorTool() {
    }

    public static Tool create() {
        Map<String, Object> schema = Schemas.object(
                Map.of("expression", Schemas.string("Arithmetic expression, e.g. (2+3)*4/2")),
                List.of("expression"));
        return new Tool("calculator", "Evaluate a basic arithmetic expression safely",
                schema, CalculatorTool::evaluate);
    }

    private static List<Content> evaluate(Map<String, Object> args) throws atlas.protocol.ToolException {
        Object expr = args.get("expression");
        if (!(expr instanceof String s) || s.isBlank()) {
            throw new atlas.protocol.ToolException("Missing 'expression' argument");
        }
        try {
            double value = new Evaluator(s).eval();
            String formatted = format(value);
            return List.of(Content.text(s + " = " + formatted));
        } catch (RuntimeException ex) {
            throw new atlas.protocol.ToolException("Could not evaluate expression: " + ex.getMessage());
        }
    }

    private static String format(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return Long.toString((long) v);
        }
        return String.format("%.4f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // ------------------------------------------------------------------
    // Minimal expression evaluator: number | ( expr ) | -expr
    // terms: * / % ; expression: + -
    // ------------------------------------------------------------------

    private static final class Evaluator {
        private final String src;
        private int pos;

        Evaluator(String src) {
            this.src = src;
            this.pos = 0;
        }

        double eval() {
            double v = expression();
            skipWs();
            if (pos < src.length()) {
                throw new RuntimeException("Unexpected character '" + src.charAt(pos) + "'");
            }
            return v;
        }

        private double expression() {
            double v = term();
            while (true) {
                skipWs();
                char c = peek();
                if (c == '+') {
                    pos++;
                    v += term();
                } else if (c == '-') {
                    pos++;
                    v -= term();
                } else {
                    return v;
                }
            }
        }

        private double term() {
            double v = factor();
            while (true) {
                skipWs();
                char c = peek();
                if (c == '*') {
                    pos++;
                    v *= factor();
                } else if (c == '/') {
                    pos++;
                    double d = factor();
                    if (d == 0) {
                        throw new RuntimeException("division by zero");
                    }
                    v /= d;
                } else if (c == '%') {
                    pos++;
                    double d = factor();
                    if (d == 0) {
                        throw new RuntimeException("modulo by zero");
                    }
                    v %= d;
                } else {
                    return v;
                }
            }
        }

        private double factor() {
            skipWs();
            char c = peek();
            if (c == '\0') {
                throw new RuntimeException("unexpected end of expression");
            }
            if (c == '+') {
                pos++;
                return factor();
            }
            if (c == '-') {
                pos++;
                return -factor();
            }
            if (c == '(') {
                pos++;
                double v = expression();
                skipWs();
                if (peek() != ')') {
                    throw new RuntimeException("missing ')'");
                }
                pos++;
                return v;
            }
            return number();
        }

        private double number() {
            skipWs();
            int start = pos;
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c) || c == '.' || c == 'e' || c == 'E'
                        || ((c == '+' || c == '-') && pos > start
                        && (src.charAt(pos - 1) == 'e' || src.charAt(pos - 1) == 'E'))) {
                    pos++;
                } else {
                    break;
                }
            }
            if (pos == start) {
                throw new RuntimeException("expected number at position " + pos);
            }
            try {
                return Double.parseDouble(src.substring(start, pos));
            } catch (NumberFormatException ex) {
                throw new RuntimeException("invalid number");
            }
        }

        private char peek() {
            skipWs();
            if (pos >= src.length()) {
                return '\0';
            }
            return src.charAt(pos);
        }

        private void skipWs() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}
