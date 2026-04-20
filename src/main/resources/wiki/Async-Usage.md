# Asynchronous Usage

For non-blocking applications (like UI apps or high-throughput servers), every API method has an `Async` counterpart. These methods do not return a value directly, but instead take a `TopGamesCallback<T>` as a parameter.

## The `TopGamesCallback` Interface

The callback has two methods you need to implement:

- `onSuccess(T result)`: This is called when the API request completes successfully. The `result` parameter contains the requested data.
- `onFailure(Exception e)`: This is called if any error occurs during the request, including network issues or API errors.

## Basic Async Call

Here is an example of how to fetch the list of unclaimed votes asynchronously.

```java
client.getUnclaimedVotesAsync(new TopGamesCallback<List<Vote>>() {
    @Override
    public void onSuccess(List<Vote> votes) {
        System.out.println("Successfully fetched " + votes.size() + " votes.");
        // Process the votes here
    }

    @Override
    public void onFailure(Exception e) {
        System.err.println("Failed to fetch votes: " + e.getMessage());
        // Handle the error
    }
});

// Your program continues to run here without waiting for the API call to finish.
```

## Handling Different Responses

The callback is generic, so you can use it for any asynchronous method.

### Checking a Vote

```java
client.checkVoteByUsernameAsync("Player1", new TopGamesCallback<Boolean>() {
    @Override
    public void onSuccess(Boolean hasVoted) {
        if (hasVoted) {
            System.out.println("Player1 has voted!");
        } else {
            System.out.println("Player1 has not voted yet.");
        }
    }

    @Override
    public void onFailure(Exception e) {
        // Handle the error
    }
});
```

### Claiming a Vote (No Return Value)

For methods that don't return data (like `claimVote`), the callback type is `Void`.

```java
client.claimVoteByUsernameAsync("Player1", new TopGamesCallback<Void>() {
    @Override
    public void onSuccess(Void result) {
        System.out.println("Vote for Player1 was successfully claimed.");
    }

    @Override
    public void onFailure(Exception e) {
        System.err.println("Could not claim vote: " + e.getMessage());
    }
});
```
