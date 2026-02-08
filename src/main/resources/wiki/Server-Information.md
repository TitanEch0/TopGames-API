# Server Information

The library allows you to retrieve detailed information about your server, including statistics and player rankings.

## Get Basic Server Info

Retrieve basic information like name, description, votes, and clicks.

```java
Server server = client.getServerInfo();
System.out.println("Server Name: " + server.getName());
System.out.println("Votes: " + server.getVotes());
```

## Get Full Server Info

Retrieve all available information, including historical statistics.

```java
Server fullServer = client.getFullServerInfo();
System.out.println("Version: " + fullServer.getVersion());
System.out.println("Players Online: " + fullServer.getPlayersOnline());

// Access stats
for (Stat stat : fullServer.getStats()) {
    System.out.println("Date: " + stat.getDate() + " - Votes: " + stat.getVotes());
}
```

## Get Server Stats Only

If you only need the statistics (votes and clicks per day).

```java
List<Stat> stats = client.getServerStats();
stats.forEach(stat -> {
    System.out.println(stat.getDate() + ": " + stat.getVotes() + " votes");
});
```

## Get Player Ranking

Retrieve the list of top voters for the current or last month.

```java
// Current month ranking
List<PlayerRanking> currentRanking = client.getPlayersRanking("current");

// Last month ranking
List<PlayerRanking> lastMonthRanking = client.getPlayersRanking("lastMonth");

for (PlayerRanking player : currentRanking) {
    System.out.println(player.getUsername() + ": " + player.getVotes() + " votes");
}
```
