package xyz.titanecho.topgamesapi;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.titanecho.topgamesapi.model.Advice;
import xyz.titanecho.topgamesapi.model.Vote;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TopGamesClientTest {

    private MockWebServer mockWebServer;
    private TopGamesClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        client = new TopGamesClient.Builder()
                .apiKey("test-api-key")
                .baseUrl(mockWebServer.url("/").toString())
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getUnclaimedVotes_Success() throws Exception {
        String jsonResponse = "{\"success\":true, \"votes\": [{\"id\":\"v1\",\"username\":\"Player1\"}]}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        List<Vote> votes = client.getUnclaimedVotes();
        
        assertNotNull(votes);
        assertEquals(1, votes.size());
        assertEquals("Player1", votes.get(0).getUsername());

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/votes/last?server_token=test-api-key"));
    }

    @Test
    void claimVoteByUsername_Success() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        client.claimVoteByUsername("Player1");

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/votes/claim-username?server_token=test-api-key&playername=Player1"));
    }
    
    @Test
    void getServerAdvices_Success() throws Exception {
        String jsonResponse = "{\"success\":true, \"advices\": [{\"id\":\"a1\",\"username\":\"Reviewer\"}]}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        List<Advice> advices = client.getServerAdvices();
        
        assertNotNull(advices);
        assertEquals(1, advices.size());
        assertEquals("Reviewer", advices.get(0).getUsername());

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/servers/test-api-key/advices"));
    }

    @Test
    void checkVoteByUsername_Success() throws Exception {
        String jsonResponse = "{\"success\":true}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        boolean hasVoted = client.checkVoteByUsername("Player1");
        assertTrue(hasVoted);

        RecordedRequest request = mockWebServer.takeRequest();
        assertTrue(request.getPath().contains("/votes/check?server_token=test-api-key&playername=Player1"));
    }

    @Test
    void checkVoteByUsernameAsync_Success() throws InterruptedException {
        String jsonResponse = "{\"success\":true}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean result = new AtomicBoolean(false);

        client.checkVoteByUsernameAsync("Player1", new TopGamesCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasVoted) {
                result.set(hasVoted);
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(result.get());
    }

    @Test
    void claimVoteBySteamIdAsync_Success() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> error = new AtomicReference<>();

        client.claimVoteBySteamIdAsync("steamid123", new TopGamesCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                error.set(e);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
    }
}
