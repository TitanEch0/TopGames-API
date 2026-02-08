package xyz.titanecho.topgamesapi.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Server {
    private String id;
    private String name;
    private String description;
    private String website;
    private String banner;
    private String logo;
    private int votes;
    private int clicks;
    private int rank;
    
    @SerializedName("players_online")
    private int playersOnline;
    
    @SerializedName("max_players")
    private int maxPlayers;
    
    private String version;
    private String ip;
    private int port;
    
    // Stats fields (optional, populated by getFullServerInfo)
    private List<Stat> stats;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }
    public int getClicks() { return clicks; }
    public void setClicks(int clicks) { this.clicks = clicks; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public int getPlayersOnline() { return playersOnline; }
    public void setPlayersOnline(int playersOnline) { this.playersOnline = playersOnline; }
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public List<Stat> getStats() { return stats; }
    public void setStats(List<Stat> stats) { this.stats = stats; }
}
