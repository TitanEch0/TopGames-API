# Asynchronous Usage

For non-blocking applications (like UI apps or high-throughput servers), every API method has an `Async` counterpart returning a `CompletableFuture`.

## Basic Async Call

```java
client.getUnclaimedVotesAsync()
    .thenAccept(votes -> {
        System.out.println("Found " + votes.size() + " new votes.");
    });
```

## Handling Errors

Use `exceptionally` to handle errors that occur during the async execution.

```java
client.getServerInfoAsync()
    .thenAccept(server -> System.out.println(server.getName()))
    .exceptionally(ex -> {
        System.err.println("Something went wrong: " + ex.getMessage());
        return null;
    });
```

## Chaining Requests

You can easily chain multiple requests. For example, check a vote and then claim it if valid.

```java
String username = "PlayerOne";

client.checkVoteByUsernameAsync(username)
    .thenCompose(hasVoted -> {
        if (hasVoted) {
            return client.claimVoteByUsernameAsync(username);
        } else {
            throw new RuntimeException("Player has not voted");
        }
    })
    .thenRun(() -> {
        System.out.println("Vote verified and claimed!");
    })
    .exceptionally(ex -> {
        System.err.println("Process failed: " + ex.getMessage());
        return null;
    });
```
