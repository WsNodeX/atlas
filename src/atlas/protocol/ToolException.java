package atlas.protocol;

/**
 * Signals a tool-level failure that should be reported to the model as an
 * {@code isError} tool result instead of aborting the server.
 */
public final class ToolException extends Exception {
    public ToolException(String message) {
        super(message);
    }

    public ToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
