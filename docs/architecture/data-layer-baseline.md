# Data Layer Baseline

Date: 2026-04-08 (updated: package layout + ports)

## Scope
- `app/src/main/java/com/chelovecheck/data`
- [`shared/data-core`](../../shared/data-core) (KMP helpers)

## Baseline metrics
- Kotlin files in data layer: `79` (main `data/`)
- Direct `data -> presentation` imports: `0` (target `0`)
- Package layout (high level):
  - `data/remote/ofd/` — подпакеты: `http`, `sanitize`, `debug`, `parse`, `telemetry`, `handlers` (не плоский список файлов).
  - `data/repository/receipt/list/` — `ReceiptListQueryBuilder`, поиск/сортировка, `ReceiptWriteService`.
- Largest files (approx):
  - `data/remote/ofd/handlers/JusanOFDHandler.kt` (~428 lines)
  - `data/analytics/pipeline/CategoryPredictionPipeline.kt` (~180 lines)
  - `data/repository/ReceiptRepositoryImpl.kt` (~170 lines)
- Quality budgets in tests:
  - max file lines: `500`
  - max function body lines: `140`

## Target thresholds (CI)
- `data -> presentation` imports: `0`
- `presentation -> data` imports: `0` ([`LayerDependencyRulesTest.presentationLayer_doesNotImportDataLayer`](../../app/src/test/java/com/chelovecheck/architecture/LayerDependencyRulesTest.kt))
- `domain` Android imports: `0`
- data file line budget: `<= 500`
- data function body budget: `<= 140`

## Notes
- Thresholds stay conservative; OFD HTML handlers remain large but isolated per provider under `handlers/`.
