# Rate Limiter

A token-bucket rate limiter in Java 17, with a Spring Boot HTTP layer that returns
`429 Too Many Requests` when a caller goes over the limit.

## How it works

Each caller (identified by the `X-API-Key` header, or by IP if absent) gets a bucket of tokens.
Every request costs one token. Tokens refill at a steady rate. No token means the request is rejected.

- `capacity` is the largest burst allowed.
- `refill-per-second` is the long-run average rate.

Settings live in `src/main/resources/application.properties`.

## Project layout

```
src/main/java/ratelimiter/
    TokenBucket, TokenBucketRateLimiter, RateLimiter, RateLimitResult   <- the limiter (plain Java)
    RateLimitInterceptor, RateLimitConfig, RateLimitProperties,
    HelloController, RateLimiterApplication                             <- Spring Boot layer
src/test/java/ratelimiter/   tests
src/main/resources/          application.properties
```

## Run

```
mvn test
mvn spring-boot:run
```

Then try it (limit is 5 burst, 1/sec):

```
for i in 1 2 3 4 5 6 7; do curl -i -H "X-API-Key: me" localhost:8080/api/hello; done
```

The first 5 return 200, the rest return 429 with a `Retry-After` header.
Response headers `X-RateLimit-Limit` and `X-RateLimit-Remaining` are included on every response.

## Design notes

- **Lazy refill:** no background thread. Tokens are topped up on each call from the elapsed time.
- **Thread safety:** `TokenBucket.tryConsume()` is `synchronized`, so two threads can never take the last token.
  One test races 200 threads for 10 tokens and checks that exactly 10 succeed.
- **Injectable clock:** tests control time directly instead of sleeping.
- **Known limitations (next steps):** buckets are never evicted (memory grows with distinct keys),
  and state is in-process only, so it does not work across multiple servers yet.

## Roadmap

1. Sliding-window algorithm and a comparison with the token bucket
2. Redis-backed limiter (atomic Lua script) so limits hold across multiple instances
3. Evict idle buckets
4. Docker + docker-compose
5. Load test with published numbers
