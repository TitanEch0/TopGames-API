package xyz.titanecho.topgamesapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.titanecho.topgamesapi.model.Advice;
import xyz.titanecho.topgamesapi.model.PlayerRanking;
import xyz.titanecho.topgamesapi.model.Server;
import xyz.titanecho.topgamesapi.model.Stat;
import xyz.titanecho.topgamesapi.model.Vote;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The main client for interacting with the Top-Games API.
 * This client is {@link Closeable} and should be closed to release resources.
 */
public class TopGamesClient implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(TopGamesClient.class);

    private final HttpUrl baseUrl;
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private final RateLimitInterceptor rateLimitInterceptor;

    private TopGamesClient(Builder builder) {
        this.baseUrl = Objects.requireNonNull(HttpUrl.parse(builder.baseUrl), "Base URL must be a valid URL");
        this.apiKey = builder.apiKey;
        this.rateLimitInterceptor = builder.rateLimitInterceptor;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ApiResponse.class, new ApiResponseDeserializer());
        this.gson = gsonBuilder.create();

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeout, builder.connectTimeoutUnit)
                .readTimeout(builder.readTimeout, builder.readTimeoutUnit);

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
            clientBuilder.addNetworkInterceptor(loggingInterceptor);
        }

        this.client = clientBuilder.build();
        log.info("TopGamesClient initialized for base URL: {}", baseUrl);
    }

    @Override
    public void close() {
        log.info("Closing TopGamesClient and releasing resources.");
        if (rateLimitInterceptor != null) {
            rateLimitInterceptor.shutdown();
        }
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        Cache cache = client.cache();
        if (cache != null) {
            try {
                cache.close();
            } catch (IOException e) {
                log.error("Failed to close OkHttp cache.", e);
            }
        }
    }

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

        public Builder addInterceptor(@NotNull Interceptor interceptor) {
            this.customInterceptors.add(interceptor);
            return this;
        }

        public TopGamesClient build() {
            Objects.requireNonNull(apiKey, "API key must be set");
            return new TopGamesClient(this);
        }
    }

    private <T> T execute(Request request, Type typeOfT) throws TopGamesException {
        log.debug("Executing synchronous request: {} {}", request.method(), request.url());
        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response, typeOfT);
        } catch (IOException e) {
            log.error("Network error for request: {}", request.url(), e);
            throw new TopGamesException("Network error occurred", e);
        }
    }

    private <T> void executeAsync(Request request, Type typeOfT, TopGamesCallback<T> callback) {
        log.debug("Executing asynchronous request: {} {}", request.method(), request.url());
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("Async network error for request: {}", request.url(), e);
                callback.onFailure(new TopGamesException("Network error occurred", e));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                try {
                    T result = handleResponse(response, typeOfT);
                    callback.onSuccess(result);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    private <T> T handleResponse(Response response, Type typeOfT) throws TopGamesException, IOException {
        try (ResponseBody body = response.body()) {
            String responseBody = body != null ? body.string() : null;

            if (!response.isSuccessful()) {
                String errorBody = responseBody != null ? responseBody : "No error body";
                log.warn("API Error on {}: {} - {}", response.request().url(), response.code(), errorBody);
                throw new TopGamesException("API Error: " + response.code() + " - " + errorBody);
            }

            log.debug("Successfully received response for: {}", response.request().url());
            if (responseBody == null || responseBody.isEmpty()) {
                if (typeOfT == Void.class) {
                    return null;
                }
                throw new TopGamesException("Response body is null");
            }

            try {
                return gson.fromJson(responseBody, typeOfT);
            } catch (JsonSyntaxException e) {
                log.error("Failed to parse JSON for request: {}", response.request().url(), e);
                throw new TopGamesException("Failed to parse JSON response", e);
            }
        }
    }

    private HttpUrl.Builder buildAuthenticatedUrl(String... pathSegments) {
        HttpUrl.Builder builder = baseUrl.newBuilder();
        for (String segment : pathSegments) {
            builder.addPathSegment(segment);
        }
        builder.addQueryParameter("server_token", this.apiKey);
        return builder;
    }

    private Request buildAuthenticatedRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();
    }

    public List<Vote> getUnclaimedVotes() throws TopGamesException {
        HttpUrl url = buildAuthenticatedUrl("votes", "last").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Vote>>>() {}.getType();
        ApiResponse<List<Vote>> response = execute(request, responseType);
        return response.getData();
    }

    public void getUnclaimedVotesAsync(TopGamesCallback<List<Vote>> callback) {
        HttpUrl url = buildAuthenticatedUrl("votes", "last").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Vote>>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<List<Vote>>>() {
            @Override
            public void onSuccess(ApiResponse<List<Vote>> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public void claimVoteByUsername(String username) throws TopGamesException {
        HttpUrl url = buildAuthenticatedUrl("votes", "claim-username")
                .addQueryParameter("playername", username)
                .build();
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        execute(request, Void.class);
    }

    public void claimVoteByUsernameAsync(String username, TopGamesCallback<Void> callback) {
        HttpUrl url = buildAuthenticatedUrl("votes", "claim-username")
                .addQueryParameter("playername", username)
                .build();
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        executeAsync(request, Void.class, callback);
    }

    public void claimVoteBySteamId(String steamId) throws TopGamesException {
        HttpUrl url = buildAuthenticatedUrl("votes", "claim-steam")
                .addQueryParameter("steam_id", steamId)
                .build();
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        execute(request, Void.class);
    }

    public void claimVoteBySteamIdAsync(String steamId, TopGamesCallback<Void> callback) {
        HttpUrl url = buildAuthenticatedUrl("votes", "claim-steam")
                .addQueryParameter("steam_id", steamId)
                .build();
        Request request = new Request.Builder().url(url).post(RequestBody.create(new byte[0])).build();
        executeAsync(request, Void.class, callback);
    }

    public Server getServerInfo() throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<Server>>() {}.getType();
        ApiResponse<Server> response = execute(request, responseType);
        return response.getData();
    }

    public void getServerInfoAsync(TopGamesCallback<Server> callback) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<Server>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<Server>>() {
            @Override
            public void onSuccess(ApiResponse<Server> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public Server getFullServerInfo() throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("full").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<Server>>() {}.getType();
        ApiResponse<Server> response = execute(request, responseType);
        return response.getData();
    }

    public void getFullServerInfoAsync(TopGamesCallback<Server> callback) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("full").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<Server>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<Server>>() {
            @Override
            public void onSuccess(ApiResponse<Server> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public List<Stat> getServerStats() throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("stats").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Stat>>>() {}.getType();
        ApiResponse<List<Stat>> response = execute(request, responseType);
        return response.getData();
    }

    public void getServerStatsAsync(TopGamesCallback<List<Stat>> callback) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("stats").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Stat>>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<List<Stat>>>() {
            @Override
            public void onSuccess(ApiResponse<List<Stat>> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public List<PlayerRanking> getPlayersRanking(String type) throws TopGamesException {
        HttpUrl.Builder urlBuilder = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("players-ranking");
        if (type != null) {
            urlBuilder.addQueryParameter("type", type);
        }
        Request request = buildAuthenticatedRequest(urlBuilder.build());
        Type responseType = new TypeToken<ApiResponse<List<PlayerRanking>>>() {}.getType();
        ApiResponse<List<PlayerRanking>> response = execute(request, responseType);
        return response.getData();
    }

    public void getPlayersRankingAsync(String type, TopGamesCallback<List<PlayerRanking>> callback) {
        HttpUrl.Builder urlBuilder = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("players-ranking");
        if (type != null) {
            urlBuilder.addQueryParameter("type", type);
        }
        Request request = buildAuthenticatedRequest(urlBuilder.build());
        Type responseType = new TypeToken<ApiResponse<List<PlayerRanking>>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<List<PlayerRanking>>>() {
            @Override
            public void onSuccess(ApiResponse<List<PlayerRanking>> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public boolean checkVoteByIP(String ip) throws TopGamesException {
        HttpUrl url = buildAuthenticatedUrl("votes", "check-ip")
                .addQueryParameter("ip", ip)
                .build();
        Request request = buildAuthenticatedRequest(url);
        ApiResponse<Object> response = execute(request, new TypeToken<ApiResponse<Object>>(){}.getType());
        return response.isSuccess();
    }

    public void checkVoteByIPAsync(String ip, TopGamesCallback<Boolean> callback) {
        HttpUrl url = buildAuthenticatedUrl("votes", "check-ip")
                .addQueryParameter("ip", ip)
                .build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<Object>>(){}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<Object>>() {
            @Override
            public void onSuccess(ApiResponse<Object> result) {
                callback.onSuccess(result.isSuccess());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public boolean checkVoteByUsername(String username) throws TopGamesException {
        HttpUrl url = buildAuthenticatedUrl("votes", "check")
                .addQueryParameter("playername", username)
                .build();
        Request request = buildAuthenticatedRequest(url);
        ApiResponse<Object> response = execute(request, new TypeToken<ApiResponse<Object>>(){}.getType());
        return response.isSuccess();
    }
    
    public void checkVoteByUsernameAsync(String username, TopGamesCallback<Boolean> callback) {
        HttpUrl url = buildAuthenticatedUrl("votes", "check")
                .addQueryParameter("playername", username)
                .build();
        Request request = buildAuthenticatedRequest(url);
        
        Type apiResponseType = new TypeToken<ApiResponse<Object>>(){}.getType();
        executeAsync(request, apiResponseType, new TopGamesCallback<ApiResponse<Object>>() {
            @Override
            public void onSuccess(ApiResponse<Object> result) {
                callback.onSuccess(result.isSuccess());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    public List<Advice> getServerAdvices() throws TopGamesException {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("advices").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Advice>>>() {}.getType();
        ApiResponse<List<Advice>> response = execute(request, responseType);
        return response.getData();
    }

    public void getServerAdvicesAsync(TopGamesCallback<List<Advice>> callback) {
        HttpUrl url = baseUrl.newBuilder().addPathSegment("servers").addPathSegment(this.apiKey).addPathSegment("advices").build();
        Request request = buildAuthenticatedRequest(url);
        Type responseType = new TypeToken<ApiResponse<List<Advice>>>() {}.getType();
        executeAsync(request, responseType, new TopGamesCallback<ApiResponse<List<Advice>>>() {
            @Override
            public void onSuccess(ApiResponse<List<Advice>> result) {
                callback.onSuccess(result.getData());
            }
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    // --- Helper Classes ---

    private static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public void setSuccess(boolean success) { this.success = success; }
        public void setMessage(String message) { this.message = message; }
        public void setData(T data) { this.data = data; }
    }

    private static class ApiResponseDeserializer implements JsonDeserializer<ApiResponse<?>> {
        @Override
        public ApiResponse<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            ApiResponse<Object> response = new ApiResponse<>();
            
            if (jsonObject.has("success")) {
                response.setSuccess(jsonObject.get("success").getAsBoolean());
            }
            if (jsonObject.has("message")) {
                response.setMessage(jsonObject.get("message").getAsString());
            }

            if (!(typeOfT instanceof ParameterizedType)) {
                return response;
            }

            Type dataType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];

            String[] dataKeys = {"votes", "server", "stats", "players", "advices"};
            for (String key : dataKeys) {
                if (jsonObject.has(key)) {
                    response.setData(context.deserialize(jsonObject.get(key), dataType));
                    break;
                }
            }
            
            return response;
        }
    }
}
