package io.horizontalsystems.bankwallet.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.*

@Database(version = 13, exportSchema = false, entities = [
    Rate::class,
    EnabledWallet::class,
    PriceAlertRecord::class,
    AccountRecord::class]
)

@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ratesDao(): RatesDao
    abstract fun walletsDao(): EnabledWalletsDao
    abstract fun accountsDao(): AccountsDao
    abstract fun priceAlertsDao(): PriceAlertsDao

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
                    .allowMainThreadQueries()
                    .addMigrations(
                            MIGRATION_8_9,
                            MIGRATION_9_10,
                            MIGRATION_10_11,
                            renameCoinDaiToSai,
                            moveCoinSettingsFromAccountToWallet
                    )
                    .build()
        }

        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AccountRecord ADD COLUMN `deleted` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS PriceAlertRecord (`coinCode` TEXT NOT NULL, `stateRaw` INTEGER NOT NULL, `lastRate` TEXT, PRIMARY KEY(`coinCode`))")
            }
        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE EnabledWallet RENAME TO TempEnabledWallet")
                database.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWallet` (`coinId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `walletOrder` INTEGER, `syncMode` TEXT, PRIMARY KEY(`coinId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
                database.execSQL("INSERT INTO EnabledWallet (`coinId`,`accountId`,`walletOrder`,`syncMode`) SELECT `coinCode`,`accountId`,`walletOrder`,`syncMode` FROM TempEnabledWallet")
                database.execSQL("DROP TABLE TempEnabledWallet")
            }
        }

        private val renameCoinDaiToSai: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("INSERT INTO EnabledWallet (`coinId`,`accountId`,`walletOrder`,`syncMode`) SELECT 'SAI',`accountId`,`walletOrder`,`syncMode` FROM EnabledWallet WHERE `coinId` = 'DAI'")
                database.execSQL("DELETE FROM EnabledWallet WHERE `coinId` = 'DAI'")
            }
        }

        private val moveCoinSettingsFromAccountToWallet: Migration = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //create new tables
                database.execSQL("""
                CREATE TABLE new_AccountRecord (
                    `deleted` INTEGER NOT NULL, 
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `origin` TEXT NOT NULL DEFAULT '',
                    `isBackedUp` INTEGER NOT NULL,
                    `words` TEXT, 
                    `salt` TEXT, 
                    `key` TEXT, 
                    `eosAccount` TEXT, 
                    PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO new_AccountRecord (`deleted`,`id`,`name`,`type`,`isBackedUp`,`words`,`salt`,`key`,`eosAccount`)
                    SELECT `deleted`,`id`,`name`,`type`,`isBackedUp`,`words`,`salt`,`key`,`eosAccount` FROM AccountRecord
                """.trimIndent())

                database.execSQL("""
                CREATE TABLE new_EnabledWallet (
                    `coinId` TEXT NOT NULL, 
                    `accountId` TEXT NOT NULL, 
                    `walletOrder` INTEGER, 
                    `syncMode` TEXT,
                    `derivation` TEXT, 
                    PRIMARY KEY(`coinId`, `accountId`), 
                    FOREIGN KEY(`accountId`) 
                    REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)
                """.trimIndent())

                database.execSQL("""
                    INSERT INTO new_EnabledWallet (`coinId`,`accountId`,`walletOrder`) 
                    SELECT `coinId`,`accountId`,`walletOrder` FROM EnabledWallet
                """.trimIndent())

                //update fields
                var oldSyncMode: String
                var oldDerivation: String? = null

                val accountsCursor = database.query("SELECT * FROM AccountRecord")
                while (accountsCursor.moveToNext()) {
                    val id = accountsCursor.getColumnIndex("id")
                    val syncMode = accountsCursor.getColumnIndex("syncMode")
                    val derivationColumnId = accountsCursor.getColumnIndex("derivation")
                    if (id >= 0 && syncMode >= 0 && derivationColumnId >= 0) {
                        val itemId = accountsCursor.getString(id)
                        oldSyncMode = accountsCursor.getString(syncMode)

                        val origin = when {
                            oldSyncMode.decapitalize() == SyncMode.New.value.decapitalize() -> AccountOrigin.Created.value
                            else -> AccountOrigin.Restored.value
                        }

                        oldDerivation = accountsCursor.getString(derivationColumnId)

                        database.execSQL("""
                            UPDATE new_AccountRecord
                            SET origin = '$origin'
                            WHERE `id` = '$itemId';
                            """.trimIndent()
                        )
                    }
                }

                val walletsCursor = database.query("SELECT * FROM EnabledWallet")
                var walletSyncMode: String? = null
                while (walletsCursor.moveToNext()) {
                    val coinIdColumnIndex = walletsCursor.getColumnIndex("coinId")
                    if (coinIdColumnIndex >= 0) {
                        val coinId = walletsCursor.getString(coinIdColumnIndex)

                        val syncModeColumnIndex = walletsCursor.getColumnIndex("syncMode")
                        if (syncModeColumnIndex >= 0){
                            walletSyncMode = walletsCursor.getString(syncModeColumnIndex)
                        }

                        if (oldDerivation != null && coinId == "BTC") {
                            database.execSQL("""
                            UPDATE new_EnabledWallet
                            SET derivation = '$oldDerivation'
                            WHERE coinId = '$coinId';
                            """.trimIndent()
                            )
                        }

                        if (coinId == "BTC" || coinId == "BCH" || coinId == "DASH") {
                            var newSyncMode = SyncMode.Fast

                            try{
                                walletSyncMode?.toLowerCase()?.capitalize()?.let {
                                    newSyncMode = SyncMode.valueOf(it)
                                }
                            } catch (e: Exception){
                                //invalid value for Enum, use default value
                            }

                            database.execSQL("""
                                UPDATE new_EnabledWallet
                                SET syncMode = '${newSyncMode.value}'
                                WHERE coinId = '$coinId';
                                """.trimIndent()
                            )
                        }
                    }
                }

                //rename tables and drop old ones
                database.execSQL("DROP TABLE AccountRecord")
                database.execSQL("DROP TABLE EnabledWallet")
                database.execSQL("ALTER TABLE new_AccountRecord RENAME TO AccountRecord")
                database.execSQL("ALTER TABLE new_EnabledWallet RENAME TO EnabledWallet")
            }
        }
    }
}
