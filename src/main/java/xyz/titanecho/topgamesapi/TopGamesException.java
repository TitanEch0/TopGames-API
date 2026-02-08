package xyz.titanecho.topgamesapi;

/**
 * A custom exception for errors that occur while interacting with the Top-Games API.
 * This can be a network error, a parsing error, or an error response from the API itself.
 */
public class TopGamesException extends Exception {
    /**
     * Constructs a new TopGamesException with the specified detail message.
     *
     * @param message the detail message.
     */
    public TopGamesException(String message) {
        super(message);
    }

    /**
     * Constructs a new TopGamesException with the specified detail message and cause.
     *
     * @param message the detail message.
     * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).
     */
    public TopGamesException(String message, Throwable cause) {
        super(message, cause);
    }
}
