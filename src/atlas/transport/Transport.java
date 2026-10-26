package atlas.transport;

import java.io.IOException;

/**
 * Bidirectional message channel between the server and its client.
 *
 * <p>The Model Context Protocol is transport agnostic. Atlas ships a
 * {@link StdioTransport} for the standard stdio deployment (where the MCP
 * client launches the server as a subprocess) and an {@link InMemoryTransport}
 * for tests and scripted demos. A transport exchanges newline-delimited JSON
 * messages; {@link #readMessage()} returns {@code null} to signal end-of-input
 * so the server knows when to stop its loop.</p>
 */
public interface Transport {

    /**
     * Block until the next message line is available, or return {@code null}
     * when the input stream is exhausted / closed.
     */
    String readMessage() throws IOException;

    /** Write a single JSON message line. */
    void writeMessage(String json) throws IOException;

    /** Release any underlying resources. */
    void close() throws IOException;
}
