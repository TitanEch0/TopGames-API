# TopGames API Java Client

A modern, robust, and feature-rich Java client for the Top-Games API. This library is designed for high performance and resilience, making it suitable for production environments.

## Features

- **Fluent Builder**: Simple and readable client configuration.
- **Sync & Async API**: Both blocking and non-blocking (`CompletableFuture`) methods are available.
- **HTTP Caching**: Reduces latency and saves API quota by caching responses.
- **Automatic Retries**: Automatically retries requests on transient network or server errors with exponential backoff.
- **Rate Limiting**: Client-side rate limiting to prevent hitting API limits and ensure fair usage.
- **Graceful Shutdown**: Implements `Closeable` for safe resource management in `try-with-resources` blocks.
- **Extensible**: Add your own custom logic (e.g., for metrics or tracing) using OkHttp interceptors.
- **Modern Logging**: Uses SLF4J for logging, allowing integration with any logging framework.

## Installation

### Maven

1.  Add the GitHub Packages repository to your `pom.xml`:
    ```xml
    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub TitanEch0 Apache Maven Packages</name>
            <url>https://maven.pkg.github.com/TitanEch0/TopGames-API</url>
        </repository>
    </repositories>
    ```

2.  Add the dependency to your `pom.xml`:
    ```xml
    <dependency>
        <groupId>io.github.titanech0</groupId>
        <artifactId>topgames-api</artifactId>
        <version>1.0.5</version>
    </dependency>
    ```

    *Note: You need to authenticate with GitHub Packages in your `settings.xml` to download the artifact.*

## Usage

### 1. Basic Usage

Create a client and make your first API call.

```java
import xyz.titanecho.topgamesapi.TopGamesClient;
import xyz.titanecho.topgamesapi.model.Game;
import xyz.titanecho.topgamesapi.TopGamesException;

// Use try-with-resources for automatic resource management
try (TopGamesClient client = new TopGamesClient.Builder()
        .apiKey("YOUR_API_KEY")
        .build()) {

    Game game = client.getGame("123");
    System.out.println("Fetched Game: " + game.getName());

} catch (TopGamesException e) {
    System.err.println("API call failed: " + e.getMessage());
}
```

### 2. Advanced Configuration

The builder allows you to enable and configure all advanced features.

```java
import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

// ...

File cacheDir = new File("./cache");

try (TopGamesClient advancedClient = new TopGamesClient.Builder()
        // Required: Your API Key
        .apiKey("YOUR_API_KEY")

        // --- Optional Features ---

        // Enable automatic retries on transient errors (up to 3 times)
        .enableRetries(3)

        // Enforce a rate limit (e.g., 5 requests per second)
        .rateLimit(5, Duration.ofSeconds(1))

        // Enable HTTP caching (10 MB in the specified directory)
        .enableHttpCache(cacheDir, 10)

        // Enable detailed HTTP logging for debugging
        .enableDebugLogging()
        
        // Add a custom interceptor for metrics or tracing
        .addInterceptor(chain -> {
            System.out.println("Custom interceptor processing request to: " + chain.request().url());
            return chain.proceed(chain.request());
        })
        .build()) {

    // All requests made with this client will now use these features
    var topGames = advancedClient.getTopGames(10, 0);
    System.out.println("Fetched " + topGames.size() + " top games.");

} catch (TopGamesException e) {
    e.printStackTrace();
}
```

### 3. Asynchronous Usage

API calls can be made asynchronously using `CompletableFuture`.

```java
try (TopGamesClient client = new TopGamesClient.Builder().apiKey("YOUR_API_KEY").build()) {

    client.getGameAsync("456")
          .thenAccept(game -> System.out.println("Async fetched game: " + game.getName()))
          .exceptionally(ex -> {
              System.err.println("Async call failed: " + ex.getMessage());
              return null;
          });

    // Keep the application alive to see the result
    Thread.sleep(2000);
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
