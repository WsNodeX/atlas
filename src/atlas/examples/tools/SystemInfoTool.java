package atlas.examples.tools;

import atlas.protocol.Content;
import atlas.protocol.Schemas;
import atlas.protocol.Tool;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exposes basic host / JVM facts (OS, CPU count, memory, Java version) as a
 * tool. Lets an agent answer environment questions or include runtime context
 * in its reasoning without scraping {@code System} properties itself.
 */
public final class SystemInfoTool {

    private SystemInfoTool() {
    }

    public static Tool create() {
        Map<String, Object> schema = Schemas.object(
                Map.of("field", Schemas.string("Optional: os | cpu | memory | java | all (default all)")),
                List.of());
        return new Tool("system_info", "Report host and JVM information",
                schema, SystemInfoTool::run);
    }

    private static List<Content> run(Map<String, Object> args) {
        String field = (String) args.getOrDefault("field", "all");
        Map<String, String> info = collect();
        if (field != null && !field.isBlank() && !field.equals("all")) {
            String v = info.get(field);
            if (v == null) {
                return List.of(Content.text("Unknown field: " + field
                        + ". Try one of: " + String.join(", ", info.keySet())));
            }
            return List.of(Content.text(field + ": " + v));
        }
        String pretty = info.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));
        return List.of(Content.text(pretty));
    }

    private static Map<String, String> collect() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        m.put("arch", System.getProperty("os.arch"));
        m.put("cpu", String.valueOf(Runtime.getRuntime().availableProcessors()));
        long maxMem = Runtime.getRuntime().maxMemory();
        m.put("memory", (maxMem / (1024 * 1024)) + " MB max heap");
        m.put("java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        m.put("pid", processId());
        return m;
    }

    private static String processId() {
        try {
            return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        } catch (RuntimeException ex) {
            return "unknown";
        }
    }
}
