# Data Core KMP Cut Map

Date: 2026-04-08

## Goal
Prepare `data` layer for incremental migration to `commonMain` without breaking Android runtime.

## Candidate `commonMain` components
- `data/analytics/pipeline/*` (except Android-only dependencies)
- `data/mapper/*` (pure mapping logic)
- `data/remote/ofd/parse` parsing primitives that do not depend on Android APIs (часть query-парсинга уже в [`SharedOfdUrlParsing`](../../shared/data-core/src/commonMain/kotlin/com/chelovecheck/shared/datacore/SharedOfdUrlParsing.kt))
- Query/policy helpers from repositories:
  - `data/repository/receipt/list/ReceiptListQueryBuilder`
  - `data/repository/receipt/list/ReceiptListSearchStrategy` policy abstractions

## Android-only (`androidMain`) components
- `data/local/*` (Room)
- `SettingsRepositoryImpl` + DataStore
- `SecureSecretsStore` (EncryptedSharedPreferences/Keystore)
- `ReceiptImageScannerImpl` (ML Kit / Camera / Android framework)
- `HttpClient` current OkHttp Android wiring

## Boundary contracts for migration
- Keep repository interfaces in `domain`.
- Add small platform abstractions when moving logic:
  - secure key-value storage
  - clock/timezone provider
  - logger facade
  - HTTP transport facade

## Stage-1 migration order
1. Move pure parser/pipeline helpers first.
2. Keep platform adapters in Android and bind through DI.
3. Move tests with logic to `commonTest`.
4. Switch Android implementations to use shared core.

## Current integration (2026-04)

- `:app` depends on `:shared:data-core`; OFD logging sanitization delegates to `SharedOfdSanitizer`; `OfdParsingCommons.parseQueryParams` delegates to `SharedOfdUrlParsing`.
- `expect object Platform` / `actual` in `jvmMain` and `iosMain` for KMP boundary smoke tests.
- Build targets enabled: `jvm`, `iosX64`, `iosArm64`, `iosSimulatorArm64` (`applyDefaultHierarchyTemplate()`).

## Why only query parsing lives in `commonMain` (not all of `OfdParsingCommons`)

[`OfdParsingCommons`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/parse/OfdParsingCommons.kt) is **split on purpose**:

- **In KMP (`SharedOfdUrlParsing`)**: parsing **query strings** from URLs — pure string work, no `java.time`, safe for `commonMain`.
- **Stays in Android `app`**: `convertQrDateToApiDate`, `parseDateTimeOrNow`, and anything that needs **`java.time`** (`LocalDateTime`, `ZoneId`, `DateTimeFormatter`) remains JVM/Android until a shared multiplatform time API is chosen for those helpers.
- **Parse fallback / telemetry**: [`OfdParseFallbackPolicy`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/parse/OfdParseFallbackPolicy.kt) ties parsing failures to [`DataTelemetry`](../../app/src/main/java/com/chelovecheck/data/telemetry/DataTelemetry.kt) and domain [`AppError`](../../app/src/main/java/com/chelovecheck/domain/model/AppError.kt). That stack is **product/Android data-layer** and is not duplicated in `shared:data-core` so the KMP module stays a thin, dependency-free helper surface.

This partial move is **complete for stage-1**, not an abandoned refactor: moving the rest requires either expect/actual clocks and logging for KMP targets or keeping date/telemetry helpers at the app boundary.
