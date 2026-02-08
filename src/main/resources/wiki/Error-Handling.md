# Error Handling

The library uses a single checked exception class: `TopGamesException`.

## When is it thrown?

1.  **Network Errors:** No internet connection, DNS failure, timeout.
2.  **API Errors:** The server returned a 4xx or 5xx status code (e.g., 401 Unauthorized, 404 Not Found).
3.  **Parsing Errors:** The server response could not be parsed as JSON.

## Handling Exceptions

The exception message usually contains the HTTP status code and the error body returned by the server.

```java
try {
    client.getGame("123");
} catch (TopGamesException e) {
    if (e.getMessage().contains("404")) {
        System.out.println("Game not found.");
    } else if (e.getMessage().contains("401")) {
        System.out.println("Invalid API Key.");
    } else {
        e.printStackTrace();
    }
}
```

## Async Error Handling

In async mode, the `TopGamesException` is wrapped inside a `CompletionException` or `ExecutionException`.

```java
client.getGameAsync("123").exceptionally(ex -> {
    // ex is likely a CompletionException
    Throwable cause = ex.getCause(); 
    if (cause instanceof TopGamesException) {
        // Handle specific API error
    }
    return null;
});
```
