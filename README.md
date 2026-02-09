# TopGames API Java Client

[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://TitanEch0.github.io/TopGames-API/)

A modern, robust, and feature-rich Java client for the Top-Games API. This library is designed for high performance and resilience, making it suitable for production environments.

## Documentation

*   **[Online Javadoc](https://TitanEch0.github.io/TopGames-API/)**: Full API reference.
*   **[Wiki](https://github.com/TitanEch0/TopGames-API/wiki)**: Detailed guides and examples.

## Features

- **Fluent Builder**: Simple and readable client configuration.
- **Sync & Async API**: Both blocking and non-blocking (`Callback`) methods are available.
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
        <version>1.1.3</version>
    </dependency>
    ```

    *Note: You need to authenticate with GitHub Packages in your `settings.xml` to download the artifact.*

## Usage

### 1. Basic Usage

Create a client and make your first API call.

```java
import xyz.titanecho.topgamesapi.TopGamesClient;
import xyz.titanecho.topgamesapi.model.Server;
import xyz.titanecho.topgamesapi.TopGamesException;

// Use try-with-resources for automatic resource management
try (TopGamesClient client = new TopGamesClient.Builder()
        .apiKey("YOUR_SERVER_TOKEN")
        .build()) {

    Server server = client.getServerInfo();
    System.out.println("Fetched Server: " + server.getName());

} catch (TopGamesException e) {
    System.err.println("API call failed: " + e.getMessage());
}
```

### 2. Advanced Configuration

The builder allows you to enable and configure all advanced features.

```java
import java.io.File;
import java.time.Duration;

File cacheDir = new File("./cache");

try (TopGamesClient advancedClient = new TopGamesClient.Builder()
        .apiKey("YOUR_SERVER_TOKEN")
        .enableRetries(3)
        .rateLimit(5, Duration.ofSeconds(1))
        .enableHttpCache(cacheDir, 10) // 10 MB cache
        .enableDebugLogging()
        .build()) {
    
    // All requests made with this client will now use these features
    var topGames = advancedClient.getPlayersRanking("current");
    System.out.println("Fetched " + topGames.size() + " players in ranking.");

} catch (TopGamesException e) {
    e.printStackTrace();
}
```

### 3. Asynchronous Usage

API calls can be made asynchronously using a `TopGamesCallback`.

```java
import xyz.titanecho.topgamesapi.TopGamesCallback;
import java.util.List;

client.getUnclaimedVotesAsync(new TopGamesCallback<List<Vote>>() {
    @Override
    public void onSuccess(List<Vote> votes) {
        System.out.println("Successfully fetched " + votes.size() + " votes.");
    }

    @Override
    public void onFailure(Exception e) {
        System.err.println("Failed to fetch votes: " + e.getMessage());
    }
});
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
