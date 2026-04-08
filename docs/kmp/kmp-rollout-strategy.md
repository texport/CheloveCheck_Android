# KMP data-core rollout and rollback

Date: 2026-04-08

## Strategy

1. **Dual-write period**: Android `app` consumes `:shared:data-core` only for pure helpers (e.g. `SharedOfdSanitizer`, future parsers). Room/DataStore/OFD handlers stay in `app` until parity tests pass per feature.
2. **Feature cutover**: Move one vertical at a time (e.g. OFD sanitizer first, then query helpers). After each step: `./gradlew :app:testDebugUnitTest :shared:data-core:allTests`.
3. **Rollback**: Revert the single merge commit for that vertical or toggle a small adapter in `app` to use local fallback (keep deprecated path for one release).

## CI gates

- `:app:testDebugUnitTest` — Android unit + Robolectric contract tests.
- `:shared:data-core:allTests` — JVM + iOS simulator tests for shared code.

## Definition of done for full cutover

- No duplicate logic between `app` `data` and `shared:data-core` for migrated components.
- `commonMain` free of `android.*` imports for migrated files (enforced by KMP + reviews).
