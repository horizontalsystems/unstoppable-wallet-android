package io.horizontalsystems.bankwallet.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.bankwallet.entities.Rate

@Database(version = 8, exportSchema = false, entities = [
    Rate::class,
    EnabledWallet::class]
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ratesDao(): RatesDao
    abstract fun walletsDao(): EnabledWalletsDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "dbBankWallet")
                    .fallbackToDestructiveMigration()
                    .addMigrations(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                            MIGRATION_4_5,
                            MIGRATION_5_6,
                            MIGRATION_6_7,
                            addNameToEnabledCoin
                    )
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
                database.execSQL("CREATE TABLE IF NOT EXISTS StorableCoin (`coinTitle` TEXT NOT NULL, `coinCode` TEXT NOT NULL, `coinType` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `walletOrder` INTEGER, PRIMARY KEY(`coinCode`))")
            }
        }

        private val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS TransactionRecord")
                database.execSQL("DROP TABLE IF EXISTS Rate")
                database.execSQL("CREATE TABLE Rate (`coinCode` TEXT NOT NULL,`currencyCode` TEXT NOT NULL,`value` REAL NOT NULL,`timestamp` INTEGER NOT NULL, `isLatest` INTEGER NOT NULL, PRIMARY KEY(`coinCode`,`currencyCode`,`timestamp`,`isLatest`))")
            }
        }

        private val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Rate")
                database.execSQL("CREATE TABLE Rate (`coinCode` TEXT NOT NULL,`currencyCode` TEXT NOT NULL,`value` TEXT NOT NULL,`timestamp` INTEGER NOT NULL, `isLatest` INTEGER NOT NULL, PRIMARY KEY(`coinCode`,`currencyCode`,`timestamp`,`isLatest`))")
            }
        }

        private val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DELETE FROM Rate")
            }
        }

        private val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS EnabledCoin (`coinCode` TEXT NOT NULL, `walletOrder` INTEGER, PRIMARY KEY(`coinCode`))")
                database.execSQL("INSERT INTO EnabledCoin (`coinCode`,`walletOrder`) SELECT `coinCode`,`order` FROM StorableCoin")
                database.execSQL("DROP TABLE StorableCoin")
            }
        }

        private val addNameToEnabledCoin: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS EnabledWallet (`coinCode` TEXT NOT NULL, `walletOrder` INTEGER, `accountName` TEXT NOT NULL, PRIMARY KEY(`coinCode`, `accountName`))")
                database.execSQL("INSERT INTO EnabledWallet(`coinCode`, `accountName`, `walletOrder`) SELECT `coinCode`, 'Mnemonic', `walletOrder`FROM EnabledCoin")
                database.execSQL("DROP TABLE EnabledCoin")
            }
        }
    }
}
