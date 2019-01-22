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
import io.horizontalsystems.bankwallet.entities.*


@Database(entities = [TransactionRecord::class, Rate::class, StorableCoin::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
                val coins = mutableListOf<Coin>()
                coins.add(Coin("Bitcoin", "BTC$suffix", CoinType.Bitcoin))
                coins.add(Coin("Bitcoin Cash", "BCH$suffix", CoinType.BitcoinCash))
                coins.add(Coin("Ethereum", "ETH$suffix", CoinType.Ethereum))

                val converter = CoinTypeConverter()
                coins.forEachIndexed { index, coin ->
                    val contentValues = ContentValues()
                    contentValues.put("coinCode", coin.code)
                    contentValues.put("coinTitle", coin.title)
                    contentValues.put("coinType", converter.coinTypeToString(coin.type))
                    contentValues.put("enabled", true)
                    contentValues.put("`order`", index)
                    database.insert("StorableCoin", SQLiteDatabase.CONFLICT_REPLACE, contentValues)
                }
            }
        }

    }

}
