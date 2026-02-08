# Advanced Configuration

The `TopGamesClient.Builder` offers several powerful features to make your application more robust and efficient.

## Rate Limiting

Prevent your application from exceeding the API's rate limits by enforcing a client-side limit.

```java
.rateLimit(5, Duration.ofSeconds(1)) // Max 5 requests per second
```

## Automatic Retries

Automatically retry requests that fail due to network issues or server errors (5xx). The client uses exponential backoff.

```java
.enableRetries(3) // Retry up to 3 times
```

## HTTP Caching

Cache responses to disk to reduce latency and save API calls.

```java
File cacheDir = new File("path/to/cache");
long cacheSize = 10; // 10 MB

.enableHttpCache(cacheDir, cacheSize)
```

## Debug Logging

Enable detailed logging of HTTP requests and responses (Headers & Body). Requires an SLF4J implementation (like Logback or SimpleLogger) in your project.

```java
.enableDebugLogging()
```

## Custom Interceptors

Add your own logic to the request pipeline, for example, for custom metrics or header injection.

```java
.addInterceptor(chain -> {
    Request request = chain.request();
    System.out.println("Requesting: " + request.url());
    return chain.proceed(request);
})
```

## Full Example

```java
TopGamesClient client = new TopGamesClient.Builder()
    .apiKey("KEY")
    .rateLimit(10, Duration.ofMinutes(1))
    .enableRetries(2)
    .enableHttpCache(new File("./cache"), 50)
    .enableDebugLogging()
    .build();
```
