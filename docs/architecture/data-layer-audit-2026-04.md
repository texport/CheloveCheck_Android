# Аудит data-слоя (повторный проход)

Дата: 2026-04-08 (обновлено: контракты и финальный KMP-аудит)

## Область

`app/src/main/java/com/chelovecheck/data` и границы общего модуля [`shared/data-core`](../kmp/data-core-cut-map.md).

Формализованные контракты (инварианты, `AppError` и коды телеметрии): **[data-layer-contracts.md](data-layer-contracts.md)**.

Готовность KMP, этап 9.8: **[../kmp/kmp-readiness-final-audit.md](../kmp/kmp-readiness-final-audit.md)**.

## Оценка по фактам (цель 9–10/10)

| Критерий | Балл | Замечания |
|----------|------|-----------|
| SOLID | 9 | Крупные классы разнесены: фасад чеков + query/search/write; общий OFD `OfdHttpExecutor` / заголовки / санитайзер; пайплайн аналитики разбит на шаги + `ResultAssemblyStep`. Большие HTML-парсеры (наследие ОФД) по-прежнему объёмны, но изолированы по провайдеру. |
| DRY | 9 | Общие части OFD HTTP/санитайзер/парсинг; `OfdHttpHeaders` / `OfdDebugLog`; SQL списков через `ReceiptListSortSqlStrategy` + `ReceiptListQueryBuilder`; `SharedOfdSanitizer` — единый источник усечения в shared-модуле. |
| KISS | 9 | Стратегии сортировки/курсора декларативны; предсказание категории читается как пайплайн шагов; настройки — `enumPreferenceFlow` / `booleanPreferenceFlow` для единообразного чтения enum/булевых значений. |
| Clean Architecture | 9–10 | Архитектурные тесты держат слои; `AnalyticsPeriod` / `AnalyticsRunState` / `AnalyticsProgress` в domain; **нет** `data → presentation` и **нет** `presentation → data` (порты `AppLocaleApplicator`, аналитика foreground); инварианты в domain-репозиториях. |
| Навигация по коду | 9 | OFD и список чеков разложены по подпакетам (`ofd/http|…`, `repository/receipt/list`). |

## Автоматические ограничители (должны оставаться зелёными)

- [`LayerDependencyRulesTest`](../../app/src/test/java/com/chelovecheck/architecture/LayerDependencyRulesTest.kt): запрет `data → presentation`, запрет импортов Android в `domain`, запрет `presentation → data`.
- [`DataLayerQualityBudgetTest`](../../app/src/test/java/com/chelovecheck/architecture/DataLayerQualityBudgetTest.kt): бюджеты размера файла/функции для `data`.

## Регрессия / golden-покрытие (примеры)

- [`ReceiptRepositoryImplRoomContractTest`](../../app/src/test/java/com/chelovecheck/data/repository/ReceiptRepositoryImplRoomContractTest.kt): Room «туда-обратно» и счётчики.
- [`ReceiptListQueryBuilderTest`](../../app/src/test/java/com/chelovecheck/data/repository/receipt/list/ReceiptListQueryBuilderTest.kt), [`ReceiptListSortSqlStrategyTest`](../../app/src/test/java/com/chelovecheck/data/repository/receipt/list/ReceiptListSortSqlStrategyTest.kt): форма SQL / постраничность.
- OFD: [`OfdHttpExecutorTest`](../../app/src/test/java/com/chelovecheck/data/remote/ofd/http/OfdHttpExecutorTest.kt), [`OfdResponseSanitizerTest`](../../app/src/test/java/com/chelovecheck/data/remote/ofd/sanitize/OfdResponseSanitizerTest.kt), [`OfdParsingCommonsGoldenTest`](../../app/src/test/java/com/chelovecheck/data/remote/ofd/parse/OfdParsingCommonsGoldenTest.kt), [`OfdHttpHeadersTest`](../../app/src/test/java/com/chelovecheck/data/remote/ofd/http/OfdHttpHeadersTest.kt).
- [`ReceiptsChangeStoreConcurrencyTest`](../../app/src/test/java/com/chelovecheck/data/repository/ReceiptsChangeStoreConcurrencyTest.kt).
- [`shared:data-core`](../../shared/data-core): `SharedOfdSanitizer`, `SharedOfdUrlParsing` + `commonTest` / multiplatform gates.

## Остаточный долг (не блокирует)

- По желанию: более широкие **контракт-тесты** по каждой реализации репозитория (фейки / in-memory), сверх уже имеющихся чеков и fetcher.
- По желанию: больше **golden-фикстур OFD** по провайдерам в `test/resources`, когда стабилизируются образцы HTML/JSON.
- KMP: расширить поверхность `expect/actual` за пределы `Platform`, когда начнётся склейка с iOS-приложением (см. документ финального KMP-аудита).

## Чеклист release gate

- Проходят layer-тесты; `rg "com.chelovecheck.presentation" app/src/main/java/com/chelovecheck/data` → 0; `rg "import com.chelovecheck.data" app/src/main/java/com/chelovecheck/presentation` → 0.
- Юнит-тесты: `./gradlew :app:testDebugUnitTest :shared:data-core:allTests`.
- Логи: OFD через санитайзер / превью; секреты в `SecureSecretsStore`, не в открытом DataStore.
