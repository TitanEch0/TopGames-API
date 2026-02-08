# Asynchronous Usage

For non-blocking applications (like UI apps or high-throughput servers), every API method has an `Async` counterpart returning a `CompletableFuture`.

## Basic Async Call

```java
client.getGameAsync("123")
    .thenAccept(game -> {
        System.out.println("Loaded: " + game.getName());
    });
```

## Handling Errors

Use `exceptionally` to handle errors that occur during the async execution.

```java
client.getGameAsync("invalid-id")
    .thenAccept(game -> System.out.println(game.getName()))
    .exceptionally(ex -> {
        System.err.println("Something went wrong: " + ex.getMessage());
        return null;
    });
```

## Chaining Requests

You can easily chain multiple requests. For example, fetch a list of top games, then fetch details for the first one.

```java
client.getTopGamesAsync(10, 0)
    .thenCompose(games -> {
        String firstGameId = games.get(0).getId();
        return client.getGameAsync(firstGameId);
    })
    .thenAccept(gameDetails -> {
        System.out.println("Details of #1 Game: " + gameDetails.getDescription());
    });
```
