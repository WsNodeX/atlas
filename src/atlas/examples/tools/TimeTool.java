package atlas.examples.tools;

import atlas.protocol.Content;
import atlas.protocol.Schemas;
import atlas.protocol.Tool;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Reports the current date and time, optionally for a specific IANA time zone.
 *
 * <p>Useful for agents that need to reason about "now" (deadlines, scheduling,
 * log timestamps) without hard-coding the host clock semantics.</p>
 */
public final class TimeTool {

    private TimeTool() {
    }

    public static Tool create() {
        Map<String, Object> schema = Schemas.object(
                Map.of("zone", Schemas.string("IANA zone id, e.g. Asia/Shanghai (defaults to UTC)")),
                List.of());
        return new Tool("current_time", "Return the current date and time",
                schema, TimeTool::run);
    }

    private static List<Content> run(Map<String, Object> args) {
        String zoneArg = (String) args.get("zone");
        ZoneId zone = ZoneId.of("UTC");
        if (zoneArg != null && !zoneArg.isBlank()) {
            try {
                zone = ZoneId.of(zoneArg);
            } catch (RuntimeException ex) {
                return List.of(Content.text("Unknown time zone: " + zoneArg
                        + " (falling back to UTC)"));
            }
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        String iso = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return List.of(Content.text(iso + " (" + zone + ")"));
    }
}
