# Контракты data-слоя (инварианты и ошибки)

Дата: 2026-04-08. Область: `app/.../com/chelovecheck/data` и общий модуль [`shared/data-core`](../kmp/data-core-cut-map.md).

## 1. Направление зависимостей

- **Domain** задаёт интерфейсы репозиториев и `AppError`; без импортов Android.
- **Data** реализует репозитории, OFD, аналитику; **не** импортирует `com.chelovecheck.presentation` (контроль в [`LayerDependencyRulesTest`](../../app/src/test/java/com/chelovecheck/architecture/LayerDependencyRulesTest.kt)).
- **Presentation** не импортирует `com.chelovecheck.data.*` — только domain/use cases; порты: [`AppLocaleApplicator`](../../app/src/main/java/com/chelovecheck/domain/repository/AppLocaleApplicator.kt), [`AnalyticsPeriodSummaryCache`](../../app/src/main/java/com/chelovecheck/domain/repository/AnalyticsPeriodSummaryCache.kt), [`AnalyticsForegroundRunCoordinator`](../../app/src/main/java/com/chelovecheck/domain/repository/AnalyticsForegroundRunCoordinator.kt), [`AnalyticsForegroundProgress`](../../app/src/main/java/com/chelovecheck/domain/repository/AnalyticsForegroundProgress.kt) (реализации в data).

## 2. Основные инварианты репозиториев

### `ReceiptRepository` ([интерфейс](../../app/src/main/java/com/chelovecheck/domain/repository/ReceiptRepository.kt))

| Аспект | Инвариант |
|--------|-----------|
| Идентичность | `fiscalSign` — стабильный ключ; повторные вставки по правилам продукта (пропуск/замена). |
| Постраничный список | `getReceiptListPage`: `cursor == null` — первая страница; порядок стабилен для заданного `ReceiptListSortOrder` (keyset / стратегии в `ReceiptListSortSqlStrategy`). |
| Запись | После изменения персистентности реализации уведомляют через `ReceiptsChangeTracker` / `ReceiptsChangeStore`, чтобы списки в UI оставались согласованными. |
| Поиск | Пустой или пробельный `searchQuery` ведёт себя как отсутствие поиска (см. `ReceiptListSearchStrategy`). |

### `SettingsRepository` ([интерфейс](../../app/src/main/java/com/chelovecheck/domain/repository/SettingsRepository.kt))

| Аспект | Инвариант |
|--------|-----------|
| Flow | Каждый `Flow` отражает текущие настройки из DataStore; подписчики получают обновления при смене ключей. |
| Секреты | API-ключи провайдеров перевода **не** хранятся в открытых строках DataStore; реализация использует `SecureSecretsStore`; в DataStore могут быть заглушки, очищаемые при миграции. |
| `translationProvider` | `setTranslationProvider` сохраняет `TranslationProvider.name`; `translationProviderConfig` читает провайдера по этому ключу (по умолчанию `GOOGLE_TRANSLATE`, если ключа нет). |
| Значения по умолчанию | Булевы флаги (тактильный отклик, промпт аналитики и т.п.) по спецификации реализации по умолчанию `true`, где так задано. |

### `ReceiptFetcher` ([интерфейс](../../app/src/main/java/com/chelovecheck/domain/repository/ReceiptFetcher.kt))

| Аспект | Инвариант |
|--------|-----------|
| Ввод URL | Корректный QR/URL для поддерживаемого ОФД; иначе `AppError.InvalidQrCode` / `UnsupportedDomain` или ошибки конкретного провайдера. |
| Сеть | Используется `OfdHttpExecutor`; сбои транспорта маппятся в `AppError.NetworkError` или `ReceiptNotFound`, если того требует семантика HTTP. |
| Капча | `ReceiptRequiresOfdVerification`, если провайдер отдаёт страницу капчи/маркетинга вместо данных чека. |

## 3. Типизированные поверхности ошибок

В domain сбои отдаются как sealed-класс [`AppError`](../../app/src/main/java/com/chelovecheck/domain/model/AppError.kt). В data добавляются **диагностические коды**, которые **не** заменяют `AppError` для UI:

| Механизм | Роль |
|----------|------|
| [`OfdReasonCode`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/telemetry/OfdReasonCode.kt) | Шаги разбора/HTTP-политики внутри обработчиков OFD (`OfdParseFallbackPolicy`, тесты, логи). |
| [`DataLayerReason`](../../app/src/main/java/com/chelovecheck/data/telemetry/DataTelemetry.kt) | Структурированные логи `event=*` для предупреждений парсера/репозитория (`OFD_PARSE`, `OFD_HTTP`, …). |

**Как маппить**

- **Итог для пользователя** → всегда `AppError` (presentation переводит в тексты).
- **Разбор инцидентов** → `OfdReasonCode` / `DataLayerReason` в логах вместе с обезличенными URL/телом (`OfdResponseSanitizer` / `SharedOfdSanitizer`).

## 4. OFD и логирование

- HTTP: [`OfdHttpExecutor`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/http/OfdHttpExecutor.kt) — в логах ошибок не должно быть «сырых» URL с секретами в query; используется санитайзер.
- Тело ответа: длинный или чувствительный текст проходит усечение/очистку (`SharedOfdSanitizer` в `shared:data-core`). Разбор query в KMP-совместимом виде: [`SharedOfdUrlParsing`](../../shared/data-core/src/commonMain/kotlin/com/chelovecheck/shared/datacore/SharedOfdUrlParsing.kt) (делегирует [`OfdParsingCommons.parseQueryParams`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/parse/OfdParsingCommons.kt)). Остальные методы `OfdParsingCommons` (даты через `java.time`) и политика [`OfdParseFallbackPolicy`](../../app/src/main/java/com/chelovecheck/data/remote/ofd/parse/OfdParseFallbackPolicy.kt) с [`DataTelemetry`](../../app/src/main/java/com/chelovecheck/data/telemetry/DataTelemetry.kt) остаются в `app` — см. раздел «Why only query parsing…» в [`data-core-cut-map.md`](../kmp/data-core-cut-map.md).
- Пошаговый отладочный дамп (`OfdDebugLog`) — политика **только для debug**; в release не включать подробные логи с пользовательскими данными.

## 5. Асинхронность и потокобезопасность (data)

- Тяжёлая работа: репозитории и классификаторы при необходимости используют явные диспетчеры (`Dispatchers.IO` / `Default` — по классу).
- In-memory кэши (`AnalyticsCacheStore`, `ReceiptsChangeStore`, LRU в памяти в `CategoryPredictionPipeline`) используют `Mutex` или атомарные обновления; критичные места покрыты целевыми тестами на конкуренцию.

## 6. Протокол изменений

При изменении контракта:

1. Обновить KDoc у затронутого интерфейса в domain.
2. Обновить этот документ и добавить/поправить регрессионный тест (golden репозитория, фикстура OFD или архитектурный тест).
