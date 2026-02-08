# TopGames API Java Client

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/YOUR_USERNAME/TopGames-API)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.titanecho.topgamesapi/TopGames-API.svg?label=Maven%20Central)](https://search.maven.org/artifact/xyz.titanecho.topgamesapi/TopGames-API)

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

1.  Add the repository to your `pom.xml` (if not on Maven Central). For GitHub Packages, for example:
    ```xml
    <repositories>
        <repository>
            <id>github</id>
            <name>GitHub YOUR_USERNAME Apache Maven Packages</name>
            <url>https://maven.pkg.github.com/YOUR_USERNAME/TopGames-API</url>
        </repository>
    </repositories>
    ```

2.  Add the dependency to your `pom.xml`:
    ```xml
    <dependency>
        <groupId>xyz.titanecho.topgamesapi</groupId>
        <artifactId>TopGames-API</artifactId>
        <version>1.0.0</version>
    </dependency>
    ```

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

## Building From Source

To build the project and install the artifacts into your local Maven repository:

```bash
mvn clean install
```

To create the distributable JARs and the local repository for deployment:

```bash
mvn deploy
```

This will generate the artifacts in the `target/mvn-repo` directory.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
```