package xyz.titanecho.topgamesapi;

import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * An OkHttp interceptor that enforces a client-side rate limit.
 */
class RateLimitInterceptor implements Interceptor {
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final Semaphore semaphore;
    private final int permits;
    private final ScheduledExecutorService scheduler;

    RateLimitInterceptor(int permits, long period, TimeUnit unit) {
        this.semaphore = new Semaphore(permits, true);
        this.permits = permits;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "RateLimit-Scheduler");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(this::replenishPermits, period, period, unit);
    }

    private void replenishPermits() {
        int permitsToRelease = permits - semaphore.availablePermits();
        if (permitsToRelease > 0) {
            semaphore.release(permitsToRelease);
            log.trace("Replenished {} permits.", permitsToRelease);
        }
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        try {
            log.trace("Waiting for rate limit permit...");
            semaphore.acquire();
            log.trace("Permit acquired. Proceeding with request.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for rate limit permit", e);
        }
        return chain.proceed(chain.request());
    }

    /**
     * Shuts down the internal scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
}
