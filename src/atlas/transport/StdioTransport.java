package atlas.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Standard stdio transport: one JSON-RPC message per line on stdin/stdout.
 *
 * <p>This is the transport the protocol specification recommends for local
 * servers. The host application spawns the server as a child process, writes
 * requests to its stdin and reads responses from its stdout, keeping stderr
 * free for diagnostics.</p>
 */
public final class StdioTransport implements Transport {

    private final BufferedReader in;
    private final PrintStream out;

    public StdioTransport(InputStream input, PrintStream output) {
        this.in = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.out = output;
    }

    /** Convenience constructor bound to the JVM's standard streams. */
    public static StdioTransport system() {
        return new StdioTransport(System.in, System.out);
    }

    @Override
    public String readMessage() throws IOException {
        String line = in.readLine();
        return line; // null at EOF
    }

    @Override
    public void writeMessage(String json) throws IOException {
        out.println(json);
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.flush();
    }
}
