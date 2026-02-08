# Getting Started

This guide shows you how to initialize the client and make your first request.

## 1. Create the Client

The `TopGamesClient` uses a builder pattern. The only required parameter is your API Key.

```java
import xyz.titanecho.topgamesapi.TopGamesClient;

TopGamesClient client = new TopGamesClient.Builder()
    .apiKey("YOUR_SECRET_API_KEY")
    .build();
```

## 2. Fetch a Game

Use the `getGame` method to retrieve details about a specific game by its ID.

```java
import xyz.titanecho.topgamesapi.model.Game;
import xyz.titanecho.topgamesapi.TopGamesException;

try {
    Game game = client.getGame("12345");
    
    System.out.println("ID: " + game.getId());
    System.out.println("Name: " + game.getName());
    System.out.println("Rank: " + game.getRank());
    
} catch (TopGamesException e) {
    System.err.println("Failed to fetch game: " + e.getMessage());
}
```

## 3. Clean Up

The client holds resources like thread pools and connection pools. Always close the client when you are done with it. The client implements `AutoCloseable`, so you can use try-with-resources:

```java
try (TopGamesClient client = new TopGamesClient.Builder().apiKey("KEY").build()) {
    // Use client here...
} // Client is automatically closed here
```
