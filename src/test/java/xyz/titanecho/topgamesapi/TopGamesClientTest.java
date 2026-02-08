package xyz.titanecho.topgamesapi;

import okhttp3.Interceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.titanecho.topgamesapi.model.PlayerRanking;
import xyz.titanecho.topgamesapi.model.Server;
import xyz.titanecho.topgamesapi.model.Stat;
import xyz.titanecho.topgamesapi.model.Vote;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TopGamesClientTest {

    private MockWebServer mockWebServer;

    @TempDir
    File tempCacheDir;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    private TopGamesClient.Builder createDefaultBuilder() {
        return new TopGamesClient.Builder()
                .apiKey("test-api-key")
                .baseUrl(mockWebServer.url("/").toString());
    }

    @Test
    void client_canBeClosed() {
        TopGamesClient client = createDefaultBuilder().build();
        assertDoesNotThrow(client::close);
    }

    @Test
    void getUnclaimedVotes_Success() throws TopGamesException {
        // The API returns a wrapper object
        String jsonResponse = "{\"code\":200, \"success\":true, \"votes\": [{\"id\":\"v1\",\"username\":\"Player1\",\"claimed\":false},{\"id\":\"v2\",\"username\":\"Player2\",\"claimed\":false}]}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            List<Vote> votes = client.getUnclaimedVotes();
            
            assertNotNull(votes);
            assertEquals(2, votes.size());
            assertEquals("Player1", votes.get(0).getUsername());
        }
    }

    @Test
    void claimVoteByUsername_Success() throws TopGamesException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            client.claimVoteByUsername("Player1");
        }

        var recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        // Verify query parameters
        assertTrue(recordedRequest.getPath().contains("/votes/claim-username"));
        assertTrue(recordedRequest.getPath().contains("playername=Player1"));
    }

    @Test
    void getServerInfo_Success() throws TopGamesException {
        String jsonResponse = "{\"code\":200, \"success\":true, \"server\": {\"id\":\"123\", \"name\":\"My Server\", \"votes\":100}}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            Server server = client.getServerInfo();
            assertNotNull(server);
            assertEquals("My Server", server.getName());
            assertEquals(100, server.getVotes());
        }
    }

    @Test
    void getServerStats_Success() throws TopGamesException {
        String jsonResponse = "{\"code\":200, \"success\":true, \"stats\": [{\"date\":\"2023-01-01\", \"votes\":10}, {\"date\":\"2023-01-02\", \"votes\":15}]}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            List<Stat> stats = client.getServerStats();
            assertNotNull(stats);
            assertEquals(2, stats.size());
            assertEquals(15, stats.get(1).getVotes());
        }
    }

    @Test
    void getPlayersRanking_Success() throws TopGamesException {
        String jsonResponse = "{\"code\":200, \"success\":true, \"players\": [{\"username\":\"ProGamer\", \"votes\":50}]}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            List<PlayerRanking> ranking = client.getPlayersRanking("current");
            assertNotNull(ranking);
            assertEquals(1, ranking.size());
            assertEquals("ProGamer", ranking.get(0).getUsername());
        }
    }
}
