package xyz.titanecho.topgamesapi;

import okhttp3.Interceptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
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
    void customInterceptor_isExecuted() throws Exception {
        AtomicBoolean interceptorCalled = new AtomicBoolean(false);
        Interceptor customInterceptor = chain -> {
            interceptorCalled.set(true);
            return chain.proceed(chain.request());
        };

        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        try (TopGamesClient client = createDefaultBuilder().addInterceptor(customInterceptor).build()) {
            client.getGame("123");
        }

        assertTrue(interceptorCalled.get(), "Custom interceptor should have been called");
        assertEquals(1, mockWebServer.getRequestCount());
    }

    @Test
    void authenticationInterceptor_addsHeaders() throws Exception {
        mockWebServer.enqueue(new MockResponse().setBody("{}"));

        try (TopGamesClient client = createDefaultBuilder().build()) {
            client.getGame("123");
        }

        var recordedRequest = mockWebServer.takeRequest();
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"));
        assertEquals("application/json", recordedRequest.getHeader("Accept"));
    }
}
