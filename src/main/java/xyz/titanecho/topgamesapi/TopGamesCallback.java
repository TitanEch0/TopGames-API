package xyz.titanecho.topgamesapi;

/**
 * A generic callback interface for handling asynchronous API responses.
 *
 * @param <T> The type of the successful response data.
 */
public interface TopGamesCallback<T> {
    /**
     * Called when the API request succeeds.
     * @param result The parsed response data.
     */
    void onSuccess(T result);

    /**
     * Called when the API request fails.
     * @param e The exception that occurred.
     */
    void onFailure(Exception e);
}
