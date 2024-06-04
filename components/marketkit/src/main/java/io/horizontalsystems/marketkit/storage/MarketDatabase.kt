package io.horizontalsystems.marketkit.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.marketkit.models.*
import java.io.BufferedReader
import java.io.InputStreamReader
//import java.util.concurrent.Executors
import java.util.logging.Logger


@Database(
    entities = [
        Coin::class,
        BlockchainEntity::class,
        TokenEntity::class,
        CoinPrice::class,
        CoinHistoricalPrice::class,
        GlobalMarketInfo::class,
        SyncerState::class,
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun coinDao(): CoinDao
    abstract fun coinPriceDao(): CoinPriceDao
    abstract fun coinHistoricalPriceDao(): CoinHistoricalPriceDao
    abstract fun globalMarketInfoDao(): GlobalMarketInfoDao
    abstract fun syncerStateDao(): SyncerStateDao
    abstract fun blockchainEntityDao(): BlockchainEntityDao
    abstract fun tokenEntityDao(): TokenEntityDao

    companion object {

        private val logger = Logger.getLogger("MarketDatabase")

        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getInstance(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MarketDatabase {
            val db = Room.databaseBuilder(context, MarketDatabase::class.java, "marketKitDatabase")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val loadedCount = loadInitialCoins(db, context)
                        logger.info("onCreate Loaded coins count: $loadedCount")
                    }

                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        super.onDestructiveMigration(db)
                        val loadedCount = loadInitialCoins(db, context)
                        logger.info("onDestructiveMigration Loaded coins count: $loadedCount")
                    }
                })
//                .setQueryCallback({ sqlQuery, bindArgs ->
//                    println("SQL Query: $sqlQuery SQL Args: $bindArgs")
//                }, Executors.newSingleThreadExecutor())
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()

            // force db creation
            db.query("select 1", null)

            return db
        }

        private fun loadInitialCoins(db: SupportSQLiteDatabase, context: Context): Int {
            val inputStream = context.assets.open("initial_coins_list")
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            var insertCount = 0

            try {
                while (bufferedReader.ready()) {
                    val insertStmt: String = bufferedReader.readLine()
                    db.execSQL(insertStmt)
                    insertCount++
                }
            } catch (error: Exception) {
                logger.warning("Error in loadInitialCoins(): ${error.message ?: error.javaClass.simpleName}")
            }

            return insertCount
        }
    }
}
