<img referrerpolicy="no-referrer-when-downgrade" src="https://static.scarf.sh/a.png?x-pxid=8fd3772b-2ddb-4042-9243-eae0fb17b66c" />

# dynamic-values

How to handle values that change between test runs.

## The Problem

UUIDs, auto-incremented IDs, timestamps, random values and other generated data are different on each run. Thus they make the comparison with an approved file fail.

## The Best Approach: Control at the Source

The best solution is to make the system deterministic. This is better than a workaround in the assertion. It needs some work on the system and on the test infrastructure. In return, the tests are more thorough and more stable, and the full test suite can run in parallel.

### Control Time

Inject a `Clock`, or an equivalent abstraction. Do not call `Instant.now()` directly. In tests, supply a fixed clock or a clock that the test controls:

```java
// System under test uses an injected clock
class TokenService {
    private final Clock clock;

    TokenService(Clock clock) { this.clock = clock; }

    Token issueToken(String userId) {
        return new Token(userId, clock.instant().plus(Duration.ofHours(1)));
    }

    boolean isExpired(Token token) {
        return clock.instant().isAfter(token.getExpiry());
    }
}

// Test: advancing the clock exercises the expiry code path deterministically
@Test
void expiredTokenIsRejected() {
    MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    TokenService service = new TokenService(clock);
    Token token = service.issueToken("user-1");

    clock.advance(Duration.ofHours(2));   // move past expiry

    assertThat(service.checkToken(token), sameJsonAsApproved());
}
```

Without control of the clock, a test of the expiry code path must wait one hour in real time, or use a fragile workaround. With control of the clock, one line moves the clock past the expiry, and the test runs the real expiry logic. The same principle applies to all behaviour that depends on time. Examples are scheduled jobs, retry backoff, TTL-based cache eviction and rate-limit windows.

### Reset All State Before Every Test

Before each test, set each part of the stateful infrastructure to a known baseline:

- **Database**: truncate tables, roll back transactions, or restore a fixed fixture snapshot
- **Caches**: clear them, or load them to a defined state
- **Message queues**: empty them, or fill them with known messages
- **ID generators**: seed or stub them, so that the generated IDs are predictable and show as real values in approved files

When each test has its own state, each test is independent and reproducible. Then the full suite can run **in parallel** safely. Without shared mutable state, the tests have no race conditions between them. You can also test code paths that are otherwise hard to reach. Examples are the first request and the nth request, an empty cache and a warm cache, and a retry after a transient failure.

### Why This Is the Best Option

With full control of the source:
- Each field in each approved file contains a real, deterministic value. You do not have to ignore, alias or custom-match anything.
- Automated tests can reach all the logic. Time-based features, cache invalidation, queue retry behaviour and scheduled tasks all become usual test cases.
- The approved files show the actual business output exactly. Thus they are more useful as a specification that changes with the code.

The three fallback strategies below are useful when you cannot control the source, or cannot control it yet. Examples are a legacy system and a third-party dependency. Another example is a test level where a full reset of the infrastructure is not practical.

---

## Fallback Strategies

### 1. Ignore

When the value is not relevant to correctness, remove it from the comparison:

```java
assertThat(actual, sameJsonAsApproved()
    .ignoring("createdAt")
    .ignoring("id"));
```

The field is not in the approved file. Use this only when the field is not important to the test. Do not use it as a general way to remove unwanted differences. Too many ignored fields make the approved file less precise, and the tests then find fewer defects.

### 2. Alias

When you want the approved file to show the field, but not its exact value:

```java
assertThat(actual, sameJsonAsApproved()
    .withAlias("2024-05-01T12:00:00Z", "<createdAt>"));
```

The approved file contains `"createdAt": "<createdAt>"`. This text is easy to read and does not change between runs. This is useful when the field must be in the output, but its exact value changes from run to run.

### 3. Custom Match

When you want to assert a structural property, for example that the value is not null, matches a pattern or is in a range:

```java
assertThat(actual, sameJsonAsApproved()
    .with("id",    notNullValue())
    .with("email", matchesPattern(".*@example\\.com")));
```

## Decision Guide

| Situation | Preferred approach |
|---|---|
| You can control the source | Fix it at the source, for full determinism |
| The value is not relevant at all | [`.ignoring()`](ignoring-fields.md) |
| The value is relevant, and you want an approved file that is easy to read | [`.withAlias()`](aliasing.md) |
| The value must satisfy a constraint | [`.with(path, matcher)`](custom-matching.md) |
| The value is stable, but needs a stronger assertion than equality | [`.alsoCheck(path, matcher)`](custom-matching.md) |

`.ignoring()`, `.withAlias()`, `.with()` and `.alsoCheck()` apply only to `sameJsonAsApproved` and `sameBeanAs`, because they operate on JSON fields. For `sameContentAsApproved`, change the string before the assertion. For example, use `String.replaceAll` to replace tokens that change with a stable placeholder. A better solution is to control the source, so that the content is deterministic.

## Related

- [ignoring-fields](ignoring-fields.md)
- [aliasing](aliasing.md)
- [custom-matching](custom-matching.md)
