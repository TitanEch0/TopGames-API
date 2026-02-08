package xyz.titanecho.topgamesapi;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.titanecho.topgamesapi.model.Game;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The main client for interacting with the Top-Games API.
 * This client is {@link Closeable} and should be closed to release resources.
 */
public class TopGamesClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(TopGamesClient.class);

    private final HttpUrl baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private final RateLimitInterceptor rateLimitInterceptor; // Keep a reference for shutdown

    private TopGamesClient(Builder builder) {
        this.baseUrl = Objects.requireNonNull(HttpUrl.parse(builder.baseUrl), "Base URL must be a valid URL");
        this.gson = new Gson();
        this.rateLimitInterceptor = builder.rateLimitInterceptor;

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeout, builder.connectTimeoutUnit)
                .readTimeout(builder.readTimeout, builder.readTimeoutUnit);

        // Add custom interceptors first
        for (Interceptor interceptor : builder.customInterceptors) {
            clientBuilder.addInterceptor(interceptor);
        }
        if (builder.retryInterceptor != null) {
            clientBuilder.addInterceptor(builder.retryInterceptor);
        }
        if (this.rateLimitInterceptor != null) {
            clientBuilder.addInterceptor(this.rateLimitInterceptor);
        }
        if (builder.cache != null) {
            clientBuilder.cache(builder.cache);
        }
        if (builder.debugLogging) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(log::debug);
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder.addNetworkInterceptor(loggingInterceptor); // Use addNetworkInterceptor for better logging
        }

        this.client = clientBuilder.build();
        log.info("TopGamesClient initialized for base URL: {}", baseUrl);
    }

    @Override
    public void close() {
        log.info("Closing TopGamesClient and releasing resources.");
        // Shutdown internal scheduler of the rate limiter
        if (rateLimitInterceptor != null) {
            rateLimitInterceptor.shutdown();
        }
        // Shutdown OkHttp's dispatcher and connection pool
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        // Close the cache
        Cache cache = client.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                log.error("Failed to close OkHttp cache.", e);
            }
        }
    }

    /**
     * A builder for creating instances of {@link TopGamesClient}.
     */
    public static class Builder {
        private String apiKey;
        private String baseUrl = "https://api.top-games.net/v1";
        private boolean debugLogging = false;
        private long connectTimeout = 10;
        private TimeUnit connectTimeoutUnit = TimeUnit.SECONDS;
        private long readTimeout = 30;
        private TimeUnit readTimeoutUnit = TimeUnit.SECONDS;
        private Cache cache = null;
        private RateLimitInterceptor rateLimitInterceptor = null;
        private RetryInterceptor retryInterceptor = null;
        private final List<Interceptor> customInterceptors = new ArrayList<>();

        public Builder apiKey(@NotNull String apiKey) {
            this.apiKey = apiKey;
            return this;
        }
        
        public Builder baseUrl(@NotNull String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder enableDebugLogging() {
            this.debugLogging = true;
            return this;
        }

        public Builder enableHttpCache(@NotNull File cacheDirectory, long maxSizeMB) {
            this.cache = new Cache(cacheDirectory, maxSizeMB * 1024 * 1024);
            return this;
        }

        public Builder rateLimit(int permits, @NotNull Duration perDuration) {
            this.rateLimitInterceptor = new RateLimitInterceptor(permits, perDuration.toMillis(), TimeUnit.MILLISECONDS);
            return this;
        }

        public Builder enableRetries(int maxRetries) {
            this.retryInterceptor = new RetryInterceptor(maxRetries, 200);
            return this;
        }

        /**
         * Adds a custom application-level interceptor.
         * @param interceptor The interceptor to add.
         * @return This builder.
         */
        public Builder addInterceptor(@NotNull Interceptor interceptor) {
            this.customInterceptors.add(interceptor);
            return this;
        }

        public TopGamesClient build() {
            Objects.requireNonNull(apiKey, "API key must be set");
            // Add authentication interceptor as the last one to ensure it has the final say on the request
            customInterceptors.add(chain -> {
                Request original = chain.request();
                Request authenticated = original.newBuilder()
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Accept", "application/json")
                        .build();
                return chain.proceed(authenticated);
            });
            return new TopGamesClient(this);
        }
    }

    // ... (rest of the methods are unchanged)
    private <T> T execute(Request request, Type typeOfT) throws TopGamesException {
        log.debug("Executing synchronous request: {} {}", request.method(), request.url());
        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, typeOfT);
        } catch (IOException e) {
            log.error("Network error for request: {}", request.url(), e);
            throw new TopGamesException("Network error occurred", e);
        }
    }

    private <T> CompletableFuture<T> executeAsync(Request request, Type typeOfT) {
        log.debug("Executing asynchronous request: {} {}", request.method(), request.url());
        CompletableFuture<T> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("Async network error for request: {}", request.url(), e);
                future.completeExceptionally(new TopGamesException("Network error occurred", e));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    T result = handleResponse(response, typeOfT);
                    future.complete(result);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                } finally {
                    response.close();
                }
            }
        });
        return future;
    }

    private <T> T handleResponse(Response response, Type typeOfT) throws TopGamesException, IOException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "No error body";
            log.warn("API Error on {}: {} - {}", response.request().url(), response.code(), errorBody);
            throw new TopGamesException("API Error: " + response.code() + " - " + errorBody);
        }

        log.debug("Successfully received response for: {}", response.request().url());
        if (response.body() == null) {
            throw new TopGamesException("Response body is null");
        }

        try {
            return gson.fromJson(response.body().string(), typeOfT);
        } catch (JsonSyntaxException e) {
            log.error("Failed to parse JSON for request: {}", response.request().url(), e);
            throw new TopGamesException("Failed to parse JSON response", e);
        }
    }

    public Game getGame(String id) throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("games").addPathSegment(id).build();
        Request request = new Request.Builder().url(url).get().build();
        return execute(request, Game.class);
    }

    public CompletableFuture<Game> getGameAsync(String id) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("games").addPathSegment(id).build();
        Request request = new Request.Builder().url(url).get().build();
        return executeAsync(request, Game.class);
    }

    public List<Game> getTopGames(int limit, int offset) throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("games")
                .addPathSegment("top")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", String.valueOf(offset))
                .build();
        Request request = new Request.Builder().url(url).get().build();
        Type listType = new TypeToken<List<Game>>() {}.getType();
        return execute(request, listType);
    }

    public CompletableFuture<List<Game>> getTopGamesAsync(int limit, int offset) {
        HttpUrl url = baseUrl.newBuilder()
                .addPathSegment("games")
                .addPathSegment("top")
                .addQueryParameter("limit", String.valueOf(limit))
                .addQueryParameter("offset", String.valueOf(offset))
                .build();
        Request request = new Request.Builder().url(url).get().build();
        Type listType = new TypeToken<List<Game>>() {}.getType();
        return executeAsync(request, listType);
    }
}
