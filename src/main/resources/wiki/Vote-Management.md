# Vote Management

Manage votes, check if a player has voted, and claim votes to reward players.

## Get Unclaimed Votes

Retrieve a list of the last votes cast for your server.

```java
List<Vote> votes = client.getUnclaimedVotes();
for (Vote vote : votes) {
    System.out.println("Vote from: " + vote.getUsername() + " at " + vote.getCreatedAt());
}
```

## Check Vote

Check if a specific player has voted.

### By IP Address

```java
boolean hasVoted = client.checkVoteByIP("192.168.1.1");
if (hasVoted) {
    System.out.println("This IP has voted!");
}
```

## Claim Votes

Mark a vote as "claimed" so you don't process it twice. This is typically done after rewarding the player.

### By Username

```java
try {
    client.claimVoteByUsername("PlayerOne");
    System.out.println("Vote claimed successfully for PlayerOne");
} catch (TopGamesException e) {
    System.err.println("Failed to claim vote: " + e.getMessage());
}
```

### By SteamID

```java
try {
    client.claimVoteBySteamId("76561198000000000");
    System.out.println("Vote claimed successfully for SteamID");
} catch (TopGamesException e) {
    System.err.println("Failed to claim vote: " + e.getMessage());
}
```
