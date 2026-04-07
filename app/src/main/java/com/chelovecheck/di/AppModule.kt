package com.chelovecheck.di

import android.content.Context
import androidx.room.Room
import com.chelovecheck.data.local.ItemCategoryCacheDao
import com.chelovecheck.data.local.ReceiptDao
import com.chelovecheck.data.local.ReceiptDatabase
import com.chelovecheck.data.local.CategoryOverrideDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chelovecheck.data.remote.HttpClient
import com.chelovecheck.data.remote.ReceiptUrlBuilderImpl
import com.chelovecheck.data.remote.ofd.JusanOFDHandler
import com.chelovecheck.data.remote.ofd.KazakhtelecomOFDHandler
import com.chelovecheck.data.remote.ofd.OFDHandlerManager
import com.chelovecheck.data.remote.ofd.TranstelecomOFDHandler
import com.chelovecheck.data.remote.ofd.FourEkRedirectOFDHandler
import com.chelovecheck.data.remote.ofd.WofdOFDHandler
import com.chelovecheck.data.remote.ofd.KaspiOFDHandler
import com.chelovecheck.data.repository.ReceiptFetcherImpl
import com.chelovecheck.data.repository.ReceiptJsonCodecImpl
import com.chelovecheck.data.repository.ReceiptRepositoryImpl
import com.chelovecheck.data.repository.CategoryRepositoryImpl
import com.chelovecheck.data.repository.CategoryOverrideRepositoryImpl
import com.chelovecheck.data.repository.RetailRepositoryImpl
import com.chelovecheck.data.repository.RetailDisplayGroupsRepositoryImpl
import com.chelovecheck.domain.repository.RetailDisplayGroupsRepository
import com.chelovecheck.data.repository.RetailCategoryHintRepositoryImpl
import com.chelovecheck.data.analytics.CategoryPredictionCacheInvalidatorImpl
import com.chelovecheck.domain.repository.CategoryPredictionCacheInvalidator
import com.chelovecheck.domain.repository.RetailCategoryHintRepository
import com.chelovecheck.data.repository.SettingsRepositoryImpl
import com.chelovecheck.data.analytics.OnnxEmbeddingService
import com.chelovecheck.data.analytics.pipeline.CategoryPredictionPipeline
import com.chelovecheck.data.analytics.OnnxModelProvider
import com.chelovecheck.data.analytics.OnnxSentenceEmbedderProvider
import com.chelovecheck.data.analytics.WordPieceTokenizerFactory
import com.chelovecheck.data.analytics.AnalyticsProgressStore
import com.chelovecheck.data.vision.ReceiptImageScannerImpl
import com.chelovecheck.data.logging.LogcatLogger
import com.chelovecheck.data.repository.ReceiptsChangeStore
import com.chelovecheck.domain.logging.AppLogger
import com.chelovecheck.domain.repository.CategoryRepository
import com.chelovecheck.domain.repository.CategoryOverrideRepository
import com.chelovecheck.domain.repository.CategoryEmbeddingService
import com.chelovecheck.domain.repository.AnalyticsProgressReporter
import com.chelovecheck.domain.repository.ReceiptFetcher
import com.chelovecheck.domain.repository.ReceiptImageScanner
import com.chelovecheck.domain.repository.ReceiptJsonCodec
import com.chelovecheck.domain.repository.ReceiptItemClassifier
import com.chelovecheck.domain.repository.ReceiptRepository
import com.chelovecheck.domain.repository.ReceiptUrlBuilder
import com.chelovecheck.domain.repository.ReceiptsChangeTracker
import com.chelovecheck.domain.repository.ExchangeRateRepository
import com.chelovecheck.domain.repository.RetailRepository
import com.chelovecheck.domain.repository.SettingsRepository
import com.chelovecheck.data.repository.ExchangeRateRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHttpClient(client: OkHttpClient): HttpClient = HttpClient(client)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReceiptDatabase {
        return Room.databaseBuilder(context, ReceiptDatabase::class.java, "receipts.db")
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
            )
            .addCallback(
                object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        migrateFts(db)
                    }
                },
            )
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()
    }

    @Provides
    fun provideReceiptDao(database: ReceiptDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideCategoryOverrideDao(database: ReceiptDatabase): CategoryOverrideDao = database.categoryOverrideDao()

    @Provides
    fun provideItemCategoryCacheDao(database: ReceiptDatabase): ItemCategoryCacheDao = database.itemCategoryCacheDao()

    @Provides
    @Singleton
    fun provideReceiptRepository(
        dao: ReceiptDao,
        receiptsChangeTracker: ReceiptsChangeTracker,
    ): ReceiptRepository = ReceiptRepositoryImpl(dao, receiptsChangeTracker)

    @Provides
    @Singleton
    fun provideReceiptJsonCodec(json: Json): ReceiptJsonCodec = ReceiptJsonCodecImpl(json)

    @Provides
    @Singleton
    fun provideRetailRepository(
        @ApplicationContext context: Context,
        json: Json,
    ): RetailRepository = RetailRepositoryImpl(context, json)

    @Provides
    @Singleton
    fun provideRetailCategoryHintRepository(
        impl: RetailCategoryHintRepositoryImpl,
    ): RetailCategoryHintRepository = impl

    @Provides
    @Singleton
    fun provideRetailDisplayGroupsRepository(
        impl: RetailDisplayGroupsRepositoryImpl,
    ): RetailDisplayGroupsRepository = impl

    @Provides
    @Singleton
    fun provideCategoryPredictionCacheInvalidator(
        impl: CategoryPredictionCacheInvalidatorImpl,
    ): CategoryPredictionCacheInvalidator = impl

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideExchangeRateRepository(impl: ExchangeRateRepositoryImpl): ExchangeRateRepository = impl

    @Provides
    @Singleton
    fun provideReceiptsChangeTracker(store: ReceiptsChangeStore): ReceiptsChangeTracker = store

    @Provides
    @Singleton
    fun provideAnalyticsProgressReporter(store: AnalyticsProgressStore): AnalyticsProgressReporter = store

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE items ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE items SET position = id")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS category_overrides (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    itemName TEXT NOT NULL,
                    categoryId TEXT NOT NULL,
                    embedding BLOB NOT NULL
                )"""
            )
            db.execSQL(
                """CREATE UNIQUE INDEX IF NOT EXISTS index_category_overrides_itemName
                    ON category_overrides(itemName)"""
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS item_category_cache (
                    nameKey TEXT NOT NULL,
                    categoryId TEXT,
                    confidence REAL NOT NULL,
                    isCertain INTEGER NOT NULL,
                    candidatesJson TEXT NOT NULL,
                    modelVersion INTEGER NOT NULL,
                    updatedAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(nameKey)
                )"""
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateFts(db)
        }
    }

    /**
     * Android's bundled SQLite is often built without the fts5 extension. FTS4 is available and
     * sufficient for item-name search; see [migrateFts].
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TRIGGER IF EXISTS items_ai")
            db.execSQL("DROP TRIGGER IF EXISTS items_au")
            db.execSQL("DROP TRIGGER IF EXISTS items_ad")
            db.execSQL("DROP TABLE IF EXISTS items_fts")
            migrateFts(db)
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE receipts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE receipts ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun migrateFts(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE VIRTUAL TABLE IF NOT EXISTS items_fts USING fts4(
                name,
                receiptFiscalSign,
                notindexed=receiptFiscalSign,
                tokenize=unicode61
            )"""
        )
        db.execSQL(
            """INSERT INTO items_fts(rowid, name, receiptFiscalSign)
                SELECT id, name, receiptFiscalSign FROM items"""
        )
        db.execSQL("DROP TRIGGER IF EXISTS items_ai")
        db.execSQL("DROP TRIGGER IF EXISTS items_au")
        db.execSQL("DROP TRIGGER IF EXISTS items_ad")
        db.execSQL(
            """CREATE TRIGGER items_ai AFTER INSERT ON items BEGIN
                INSERT INTO items_fts(rowid, name, receiptFiscalSign)
                VALUES (new.id, new.name, new.receiptFiscalSign);
            END"""
        )
        db.execSQL(
            """CREATE TRIGGER items_ad AFTER DELETE ON items BEGIN
                DELETE FROM items_fts WHERE rowid = old.id;
            END"""
        )
        db.execSQL(
            """CREATE TRIGGER items_au AFTER UPDATE ON items BEGIN
                DELETE FROM items_fts WHERE rowid = old.id;
                INSERT INTO items_fts(rowid, name, receiptFiscalSign)
                VALUES (new.id, new.name, new.receiptFiscalSign);
            END"""
        )
    }

    @Provides
    @Singleton
    fun provideOfdHandlerManager(
        kazakhtelecomOFDHandler: KazakhtelecomOFDHandler,
        transtelecomOFDHandler: TranstelecomOFDHandler,
        jusanOFDHandler: JusanOFDHandler,
        fourEkRedirectOFDHandler: FourEkRedirectOFDHandler,
        wofdOFDHandler: WofdOFDHandler,
        kaspiOFDHandler: KaspiOFDHandler,
    ): OFDHandlerManager {
        return OFDHandlerManager(
            mapOf(
                "4ek.kz" to fourEkRedirectOFDHandler,
                "consumer.oofd.kz" to kazakhtelecomOFDHandler,
                "oofd.kz" to kazakhtelecomOFDHandler,
                "ofd1.kz" to transtelecomOFDHandler,
                "87.255.215.96" to transtelecomOFDHandler,
                "consumer.kofd.kz" to jusanOFDHandler,
                "kofd.kz" to jusanOFDHandler,
                "consumer.wofd.kz" to wofdOFDHandler,
                "cabinet.wofd.kz" to wofdOFDHandler,
                "wofd.kz" to wofdOFDHandler,
                "receipt.kaspi.kz" to kaspiOFDHandler,
            )
        )
    }

    @Provides
    @Singleton
    fun provideReceiptFetcher(handlerManager: OFDHandlerManager): ReceiptFetcher {
        return ReceiptFetcherImpl(handlerManager)
    }

    @Provides
    @Singleton
    fun provideReceiptUrlBuilder(): ReceiptUrlBuilder = ReceiptUrlBuilderImpl()

    @Provides
    @Singleton
    fun provideReceiptImageScanner(): ReceiptImageScanner = ReceiptImageScannerImpl()

    @Provides
    @Singleton
    fun provideReceiptItemClassifier(
        pipeline: CategoryPredictionPipeline,
    ): ReceiptItemClassifier = pipeline

    @Provides
    @Singleton
    fun provideCategoryRepository(
        @ApplicationContext context: Context,
        json: Json,
    ): CategoryRepository = CategoryRepositoryImpl(context, json)

    @Provides
    @Singleton
    fun provideCategoryOverrideRepository(dao: CategoryOverrideDao): CategoryOverrideRepository {
        return CategoryOverrideRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideTokenizerFactory(@ApplicationContext context: Context): WordPieceTokenizerFactory {
        return WordPieceTokenizerFactory(context)
    }

    @Provides
    @Singleton
    fun provideOnnxModelProvider(
        @ApplicationContext context: Context,
        logger: AppLogger,
    ): OnnxModelProvider = OnnxModelProvider(context, logger)

    @Provides
    @Singleton
    fun provideSentenceEmbedderProvider(
        modelProvider: OnnxModelProvider,
        tokenizerFactory: WordPieceTokenizerFactory,
        logger: AppLogger,
        progressReporter: AnalyticsProgressReporter,
    ): OnnxSentenceEmbedderProvider = OnnxSentenceEmbedderProvider(
        modelProvider,
        tokenizerFactory,
        logger,
        progressReporter,
    )

    @Provides
    @Singleton
    fun provideCategoryEmbeddingService(
        embedderProvider: OnnxSentenceEmbedderProvider,
    ): CategoryEmbeddingService = OnnxEmbeddingService(embedderProvider)

    @Provides
    @Singleton
    fun provideLogger(settingsRepository: SettingsRepository): AppLogger = LogcatLogger(settingsRepository)
}
