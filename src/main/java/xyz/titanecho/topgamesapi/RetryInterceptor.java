package xyz.titanecho.topgamesapi;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * An OkHttp interceptor that retries requests on failure.
 */
class RetryInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);
    private final int maxRetries;
    private final long initialDelayMs;

    RetryInterceptor(int maxRetries, long initialDelayMs) {
        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException exception = null;
        int tryCount = 0;
        long delayMs = initialDelayMs;

        while (tryCount <= maxRetries) {
            tryCount++;
            try {
                response = chain.proceed(request);
                if (response.isSuccessful() || !isRetryable(response)) {
                    return response;
                }
            } catch (IOException e) {
                exception = e;
                log.warn("Request failed due to IOException on try #{}. Retrying...", tryCount, e);
            }

            if (response != null) {
                // Close the previous unsuccessful response body to prevent resource leaks
                response.close();
            }

            if (tryCount > maxRetries) {
                break;
            }

            try {
                log.debug("Waiting {}ms before retry #{}", delayMs, tryCount);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted during retry backoff", e);
            }
            // Exponential backoff
            delayMs *= 2;
        }

        if (exception != null) {
            throw exception;
        }
        // Should not happen if response is not null, but as a fallback
        return response;
    }

    private boolean isRetryable(Response response) {
        // Retry on server errors (5xx)
        return response.code() >= 500 && response.code() < 600;
    }
}
