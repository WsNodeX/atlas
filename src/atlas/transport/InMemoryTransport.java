package atlas.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * In-process transport for tests and scripted demos.
 *
 * <p>A driver feeds request lines with {@link #feed(String)} and then calls
 * {@link #end()} to close the input side. The server's read loop terminates
 * naturally when the queue is drained, exactly like an EOF on a real stream.
 * Every message the server writes is captured and available via
 * {@link #getOutgoing()}, which makes assertions in unit tests trivial.</p>
 */
public final class InMemoryTransport implements Transport {

    private static final String END = "__ATLAS_END__";

    private final LinkedBlockingQueue<String> incoming = new LinkedBlockingQueue<>();
    private final List<String> outgoing = new ArrayList<>();

    /** Queue a request/notification line for the server to read. */
    public void feed(String line) {
        incoming.add(line);
    }

    /** Signal end-of-input (equivalent to closing the underlying stream). */
    public void end() {
        incoming.add(END);
    }

    /** All messages the server has written so far, in order. */
    public List<String> getOutgoing() {
        return new ArrayList<>(outgoing);
    }

    @Override
    public String readMessage() throws IOException {
        try {
            String s = incoming.take();
            return END.equals(s) ? null : s;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void writeMessage(String json) throws IOException {
        synchronized (outgoing) {
            outgoing.add(json);
        }
    }

    @Override
    public void close() throws IOException {
        // no-op for in-memory
    }
}
