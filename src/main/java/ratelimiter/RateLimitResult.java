package ratelimiter;

/**
 * The answer to "may this request go through?".
 *
 * @param allowed           true if the request is allowed
 * @param remaining         whole tokens left in the bucket after this decision
 * @param retryAfterMillis  how long to wait before a token is available (0 if allowed)
 */
public record RateLimitResult(boolean allowed, long remaining, long retryAfterMillis) {
}
