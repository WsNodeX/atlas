package atlas.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builders for the capability and implementation objects exchanged during the
 * {@code initialize} handshake.
 *
 * <p>In the protocol an empty object ({@code {}}) under a capability key means
 * "this capability is supported"; the absence of the key means it is not. Atlas
 * only advertises the capabilities it actually implements, which keeps clients
 * from requesting features the server cannot honour.</p>
 */
public final class Capabilities {

    private Capabilities() {
    }

    public static Map<String, Object> serverCapabilities(boolean tools, boolean resources,
                                                        boolean prompts, boolean logging) {
        Map<String, Object> caps = new LinkedHashMap<>();
        if (tools) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("listChanged", false);
            caps.put("tools", t);
        }
        if (resources) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("subscribe", false);
            r.put("listChanged", false);
            caps.put("resources", r);
        }
        if (prompts) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("listChanged", false);
            caps.put("prompts", p);
        }
        if (logging) {
            caps.put("logging", new LinkedHashMap<>());
        }
        return caps;
    }

    public static Map<String, Object> implementation(String name, String version) {
        Map<String, Object> impl = new LinkedHashMap<>();
        impl.put("name", name);
        impl.put("version", version);
        return impl;
    }
}
