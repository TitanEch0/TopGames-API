# Getting Started

This guide shows you how to initialize the client and make your first request.

## 1. Create the Client

The `TopGamesClient` uses a builder pattern. The only required parameter is your API Key (Server Token).

```java
import xyz.titanecho.topgamesapi.TopGamesClient;

TopGamesClient client = new TopGamesClient.Builder()
    .apiKey("YOUR_SERVER_TOKEN")
    .build();
```

## 2. Fetch Server Info

A simple call to verify everything is working.

```java
import xyz.titanecho.topgamesapi.model.Server;
import xyz.titanecho.topgamesapi.TopGamesException;

try {
    Server server = client.getServerInfo();
    System.out.println("Connected to: " + server.getName());
    System.out.println("Current Rank: " + server.getRank());
    
} catch (TopGamesException e) {
    System.err.println("Failed to fetch server info: " + e.getMessage());
}
```

## 3. Clean Up

The client holds resources like thread pools and connection pools. Always close the client when you are done with it. The client implements `AutoCloseable`, so you can use try-with-resources:

```java
try (TopGamesClient client = new TopGamesClient.Builder().apiKey("KEY").build()) {
    // Use client here...
} // Client is automatically closed here
```
