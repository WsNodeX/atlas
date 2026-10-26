package atlas.examples.tools;

import atlas.protocol.Content;
import atlas.protocol.Schemas;
import atlas.protocol.Tool;

import java.util.List;
import java.util.Map;

/**
 * Echoes back the supplied text. Deceptively simple, but it is the canonical
 * "hello world" tool and a handy end-to-end connectivity probe for any MCP
 * client wiring up a new server.
 */
public final class EchoTool {

    private EchoTool() {
    }

    public static Tool create() {
        Map<String, Object> schema = Schemas.object(
                Map.of("text", Schemas.string("The text to echo back")),
                List.of("text"));
        return new Tool("echo", "Echo the provided text back to the caller",
                schema, EchoTool::run);
    }

    private static List<Content> run(Map<String, Object> args) {
        Object text = args.getOrDefault("text", "");
        return List.of(Content.text(String.valueOf(text)));
    }
}
