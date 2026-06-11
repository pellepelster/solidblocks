# Conventions for Claude

## Test method naming (Kotlin)

Name `@Test` methods as backtick-quoted natural-language sentences describing the
behavior under test — lowercase plain English, read as `<subject> <verb> <expectation>`:

```kotlin
@Test
fun `service needs secret provider`() { ... }

@Test
fun `waitFor returns null when callback always returns null`() { ... }
```

Rules:
- No `test` prefix and no camelCase — the `@Test` annotation already marks it as a test.
- Lowercase the words; keep all-caps acronyms as-is (`SSH`, `RSA`, `S3`).
- Exceptions, kept verbatim:
  - `fun testFlow(context: SolidblocksTestContext)` — reserved name for the single
    provisioning-flow entry point in integration test classes.
  - Non-`@Test` helper functions (e.g. `createManager`, `setup`) keep normal camelCase.

This is the established style in `solidblocks-cloud`; apply it in all Kotlin modules.
