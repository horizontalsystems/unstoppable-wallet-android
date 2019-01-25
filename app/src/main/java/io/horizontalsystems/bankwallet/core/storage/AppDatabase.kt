package io.horizontalsystems.bankwallet.core.storage

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.migration.Migration
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.StorableCoin


@Database(entities = [Rate::class, StorableCoin::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ratesDao(): RatesDao

    abstract fun coinsDao(): StorableCoinsDao


    companion object {

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "dbBankWallet")
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //renaming column coin to coinCode
                database.execSQL("ALTER TABLE TransactionRecord RENAME TO TransactionTemp")
                database.execSQL("CREATE TABLE TransactionRecord (`transactionHash` TEXT NOT NULL PRIMARY KEY,`blockHeight` INTEGER NOT NULL,`coinCode` TEXT NOT NULL,`amount` REAL NOT NULL,`timestamp` INTEGER NOT NULL,`rate` REAL NOT NULL,`from` TEXT NOT NULL,`to` TEXT NOT NULL)")
                database.execSQL("INSERT INTO TransactionRecord (`transactionHash`,`blockHeight`,`coinCode`,`amount`,`timestamp`,`rate`,`from`,`to`) SELECT `transactionHash`,`blockHeight`,`coin`,`amount`,`timestamp`,`rate`,`from`,`to` FROM TransactionTemp")
                database.execSQL("DROP TABLE TransactionTemp")

                database.execSQL("ALTER TABLE Rate RENAME TO RateTemp")
                database.execSQL("CREATE TABLE Rate (`coinCode` TEXT NOT NULL,`currencyCode` TEXT NOT NULL,`value` REAL NOT NULL,`timestamp` INTEGER NOT NULL, PRIMARY KEY(`coinCode`,`currencyCode`))")
                database.execSQL("INSERT INTO Rate (`coinCode`,`currencyCode`,`value`,`timestamp`) SELECT `coin`,`currencyCode`,`value`,`timestamp` FROM RateTemp")
                database.execSQL("DROP TABLE RateTemp")
            }
        }

        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //create new table Coin
                database.execSQL("CREATE TABLE IF NOT EXISTS StorableCoin (`coinTitle` TEXT NOT NULL, `coinCode` TEXT NOT NULL, `coinType` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `order` INTEGER, PRIMARY KEY(`coinCode`))")
                //save default coin
                val suffix = if (BuildConfig.testMode) "t" else ""

                val bitcoinValues = ContentValues()
                bitcoinValues.put("coinCode", "BTC$suffix")
                bitcoinValues.put("coinTitle", "Bitcoin")
                bitcoinValues.put("coinType", "bitcoin_key")
                bitcoinValues.put("enabled", true)
                bitcoinValues.put("`order`", 0)

                val bitcoinCashValues = ContentValues()
                bitcoinCashValues.put("coinCode", "BCH$suffix")
                bitcoinCashValues.put("coinTitle", "Bitcoin Cash")
                bitcoinCashValues.put("coinType", "bitcoin_cash_key")
                bitcoinCashValues.put("enabled", true)
                bitcoinCashValues.put("`order`", 1)

                val ethereumValues = ContentValues()
                ethereumValues.put("coinCode", "ETH$suffix")
                ethereumValues.put("coinTitle", "Ethereum")
                ethereumValues.put("coinType", "ethereum_key")
                ethereumValues.put("enabled", true)
                ethereumValues.put("`order`", 2)

                database.insert("StorableCoin", SQLiteDatabase.CONFLICT_REPLACE, bitcoinValues)
                database.insert("StorableCoin", SQLiteDatabase.CONFLICT_REPLACE, bitcoinCashValues)
                database.insert("StorableCoin", SQLiteDatabase.CONFLICT_REPLACE, ethereumValues)
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS TransactionRecord")
                database.execSQL("DROP TABLE IF EXISTS Rate")
                database.execSQL("CREATE TABLE Rate (`coinCode` TEXT NOT NULL,`currencyCode` TEXT NOT NULL,`value` REAL NOT NULL,`timestamp` INTEGER NOT NULL, `isLatest` INTEGER NOT NULL, PRIMARY KEY(`coinCode`,`currencyCode`,`timestamp`,`isLatest`))")
            }
        }

    }

}
