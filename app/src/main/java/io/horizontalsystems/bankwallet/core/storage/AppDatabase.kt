package io.horizontalsystems.bankwallet.core.storage

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.PriceAlertManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType

@Database(version = 30, exportSchema = false, entities = [
    EnabledWallet::class,
    PriceAlert::class,
    AccountRecord::class,
    BlockchainSetting::class,
    SubscriptionJob::class,
    LogEntry::class,
    FavoriteCoin::class,
    WalletConnectSession::class,
])

@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun walletsDao(): EnabledWalletsDao
    abstract fun accountsDao(): AccountsDao
    abstract fun priceAlertsDao(): PriceAlertsDao
    abstract fun blockchainSettingDao(): BlockchainSettingDao
    abstract fun subscriptionJobDao(): SubscriptionJobDao
    abstract fun logsDao(): LogsDao
    abstract fun marketFavoritesDao(): MarketFavoritesDao
    abstract fun walletConnectSessionDao(): WalletConnectSessionDao

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
                            moveCoinSettingsFromAccountToWallet,
                            storeBipToPreferences,
                            addBlockchainSettingsTable,
                            addIndexToEnableWallet,
                            updateBchSyncMode,
                            addCoinRecordTable,
                            removeRateStorageTable,
                            addNotificationTables,
                            addLogsTable,
                            updateEthereumCommunicationMode,
                            addBirthdayHeightToAccount,
                            addBep2SymbolToRecord,
                            MIGRATION_24_25,
                            MIGRATION_25_26,
                            MIGRATION_26_27,
                            MIGRATION_27_28,
                            MIGRATION_28_29,
                            MIGRATION_29_30,
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
                        if (syncModeColumnIndex >= 0) {
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

                            try {
                                walletSyncMode?.toLowerCase()?.capitalize()?.let {
                                    newSyncMode = SyncMode.valueOf(it)
                                }
                            } catch (e: Exception) {
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

        private val storeBipToPreferences: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val walletsCursor = database.query("SELECT * FROM EnabledWallet")
                while (walletsCursor.moveToNext()) {
                    val coinIdColumnIndex = walletsCursor.getColumnIndex("coinId")
                    if (coinIdColumnIndex >= 0) {
                        val coinId = walletsCursor.getString(coinIdColumnIndex)

                        if (coinId == "BTC") {
                            val derivationColumnIndex = walletsCursor.getColumnIndex("derivation")
                            if (derivationColumnIndex >= 0) {
                                val walletDerivation = walletsCursor.getString(derivationColumnIndex)
                                if (walletDerivation != null) {
                                    try {
                                        val derivation = AccountType.Derivation.valueOf(walletDerivation)
                                        App.localStorage.bitcoinDerivation = derivation
                                    } catch (e: Exception) {
                                        Log.e("AppDatabase", "migration 13-14 exception", e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private val addBlockchainSettingsTable: Migration = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {

                //remove unused field from EnabledWallet
                database.execSQL("ALTER TABLE EnabledWallet RENAME TO TempEnabledWallet")
                database.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWallet` (`coinId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `walletOrder` INTEGER, PRIMARY KEY(`coinId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
                database.execSQL("INSERT INTO EnabledWallet (`coinId`,`accountId`,`walletOrder`) SELECT `coinId`,`accountId`,`walletOrder` FROM TempEnabledWallet")
                database.execSQL("DROP TABLE TempEnabledWallet")

                //add new table
                database.execSQL("CREATE TABLE IF NOT EXISTS `BlockchainSetting` (`coinType` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`coinType`, `key`))")

                //write settings from SharedPreferences to new table
                val dbConverter = DatabaseConverters()
                val walletsCursor = database.query("SELECT * FROM EnabledWallet")
                while (walletsCursor.moveToNext()) {
                    val coinIdColumnIndex = walletsCursor.getColumnIndex("coinId")
                    if (coinIdColumnIndex >= 0) {
                        val coinId = walletsCursor.getString(coinIdColumnIndex)
                        val syncMode = App.localStorage.syncMode ?: SyncMode.Fast
                        var syncModeStr: String? = syncMode.value
                        var coinTypeStr: String? = null
                        var derivationStr: String? = null
                        var communicationStr: String? = null

                        when (coinId) {
                            "BTC" -> {
                                coinTypeStr = dbConverter.fromCoinType(CoinType.Bitcoin)
                                derivationStr = (App.localStorage.bitcoinDerivation
                                        ?: AccountType.Derivation.bip49).value

                            }
                            "BCH" -> {
                                coinTypeStr = dbConverter.fromCoinType(CoinType.BitcoinCash)
                            }
                            "DASH" -> {
                                coinTypeStr = dbConverter.fromCoinType(CoinType.Dash)
                            }

                            "ETH" -> {
                                coinTypeStr = dbConverter.fromCoinType(CoinType.Ethereum)
                                syncModeStr = null
                                communicationStr = CommunicationMode.Infura.value
                            }
                        }

                        coinTypeStr?.let { saveSettings(database, it, derivationStr, syncModeStr, communicationStr) }
                    }
                }
            }

            private fun saveSettings(database: SupportSQLiteDatabase, coinType: String, derivation: String?, syncMode: String?, communication: String?) {
                derivation?.let {
                    insertIntoBlockchainSetting(database, coinType, BlockchainSettingsStorage.derivationSettingKey, it)
                }

                syncMode?.let {
                    insertIntoBlockchainSetting(database, coinType, BlockchainSettingsStorage.syncModeSettingKey, it)
                }

                communication?.let {
                    insertIntoBlockchainSetting(database, coinType, BlockchainSettingsStorage.ethereumRpcModeSettingKey, it)
                }

            }

            private fun insertIntoBlockchainSetting(database: SupportSQLiteDatabase, coinType: String, key: String, value: String) {
                try {
                    database.execSQL("""
                                                INSERT INTO BlockchainSetting (`coinType`,`key`,`value`) 
                                                VALUES ('$coinType', '$key', '$value')
                                                """.trimIndent())
                } catch (ex: SQLiteConstraintException) {
                    // Primary key violation exception can occur, because settings are inserted for each coin in EnabledWallet for specific Account.
                    // But since wallets in EnabledWallet are deleted asynchronously on next application start by AccountCleaner, there can be more than one wallet for the same coinId.
                    // We should ignore such exceptions.
                }

            }
        }

        private val addIndexToEnableWallet: Migration = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_EnabledWallet_accountId` ON `EnabledWallet` (`accountId`)")
            }
        }

        private val updateBchSyncMode: Migration = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    UPDATE BlockchainSetting 
                    SET value = '${SyncMode.Slow.value}' 
                    WHERE coinType = 'bitcoincash' AND `key` = 'sync_mode' AND value = '${SyncMode.Fast.value}';
                    """.trimIndent())
            }
        }

        private val addCoinRecordTable: Migration = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS CoinRecord (
                    `coinId` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `code` TEXT NOT NULL, 
                    `decimal` INTEGER NOT NULL, 
                    `tokenType` TEXT NOT NULL, 
                    `erc20Address` TEXT, 
                    PRIMARY KEY(`coinId`)
                    )
                    """.trimIndent())
            }
        }

        private val removeRateStorageTable: Migration = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS Rate")
            }
        }

        private val addNotificationTables: Migration = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS PriceAlert (`coinId` TEXT NOT NULL, `changeState` TEXT NOT NULL, `trendState` TEXT NOT NULL, PRIMARY KEY(`coinId`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS SubscriptionJob (`coinId` TEXT NOT NULL, `topicName` TEXT NOT NULL, `stateType` TEXT NOT NULL, `jobType` TEXT NOT NULL, PRIMARY KEY(`coinId`, `stateType`))")

                val dbConverter = DatabaseConverters()
                val alertsCursor = database.query("SELECT * FROM PriceAlertRecord")
                while (alertsCursor.moveToNext()) {
                    val coinCodeColumnIndex = alertsCursor.getColumnIndex("coinCode")
                    val changeStateColumnIndex = alertsCursor.getColumnIndex("stateRaw")
                    if (coinCodeColumnIndex >= 0 && changeStateColumnIndex >= 0) {
                        val coinCode = alertsCursor.getString(coinCodeColumnIndex)
                        val changeStateOld = alertsCursor.getInt(changeStateColumnIndex)

                        val newState = if (changeStateOld == 2 || changeStateOld == 3) {
                            PriceAlert.ChangeState.PERCENT_2
                        } else if (changeStateOld == 5) {
                            PriceAlert.ChangeState.PERCENT_5
                        } else {
                            continue
                        }

                        val changeStateValue = dbConverter.fromChangeState(newState)

                        database.execSQL("""
                                                INSERT INTO PriceAlert (`coinId`,`changeState`,`trendState`) 
                                                VALUES ('$coinCode', '$changeStateValue', 'off')
                                                """.trimIndent())

                        val topic = "${coinCode}_24hour_${newState.value}percent"
                        database.execSQL("""
                                                INSERT INTO SubscriptionJob (`coinId`,`topicName`,`stateType`,`jobType`) 
                                                VALUES ('$coinCode', '$topic', 'change', 'subscribe')
                                                """.trimIndent())
                    }
                }

                database.execSQL("DROP TABLE IF EXISTS PriceAlertRecord")
            }
        }

        private val addLogsTable: Migration = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `LogEntry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `date` INTEGER NOT NULL, `level` INTEGER NOT NULL, `actionId` TEXT NOT NULL, `message` TEXT NOT NULL)")
            }
        }

        private val updateEthereumCommunicationMode: Migration = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    UPDATE BlockchainSetting 
                    SET value = '${CommunicationMode.Infura.value}' 
                    WHERE coinType = 'ethereum' AND `key` = 'communication' AND value = '${CommunicationMode.Incubed.value}';
                    """.trimIndent())
            }
        }

        private val addBirthdayHeightToAccount: Migration = object : Migration(22, 23) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE AccountRecord ADD COLUMN `birthdayHeight` INTEGER")
            }
        }

        private val addBep2SymbolToRecord: Migration = object : Migration(23, 24) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE CoinRecord ADD COLUMN `bep2Symbol` TEXT")
            }
        }

        private val MIGRATION_24_25: Migration = object : Migration(24, 25) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // addFavoriteCoinsTable 24, 25
                database.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteCoin` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `code` TEXT NOT NULL)")

                // addCoinTypeBlockchainSettingForBitcoinCash 25, 26
                val walletsCursor = database.query("SELECT * FROM EnabledWallet WHERE coinId = 'BCH'")
                while (walletsCursor.count > 0) {
                    database.execSQL("""
                                        INSERT INTO BlockchainSetting (`coinType`,`key`,`value`) 
                                        VALUES ('bitcoincash', 'network_coin_type', 'type0')
                                        """.trimIndent())
                    return
                }
            }
        }

        private val MIGRATION_25_26: Migration = object : Migration(25, 26) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // deleteEosColumnFromAccountRecord
                database.execSQL("ALTER TABLE AccountRecord RENAME TO TempAccountRecord")
                database.execSQL("""
                CREATE TABLE AccountRecord (
                    `deleted` INTEGER NOT NULL, 
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `origin` TEXT NOT NULL DEFAULT '',
                    `isBackedUp` INTEGER NOT NULL,
                    `words` TEXT, 
                    `salt` TEXT, 
                    `key` TEXT, 
                    `birthdayHeight` INTEGER, 
                    PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO AccountRecord (`deleted`,`id`,`name`,`type`,`origin`,`isBackedUp`,`words`,`salt`,`key`,`birthdayHeight`)
                    SELECT `deleted`,`id`,`name`,`type`,`origin`,`isBackedUp`,`words`,`salt`,`key`,`birthdayHeight` FROM TempAccountRecord
                    WHERE `type` != 'eos'
                """.trimIndent())
                database.execSQL("DROP TABLE TempAccountRecord")
            }
        }

        private val MIGRATION_26_27: Migration = object : Migration(26, 27) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL( "CREATE TABLE IF NOT EXISTS `WalletConnectSession` (`chainId` INTEGER NOT NULL, `accountId` TEXT NOT NULL, `session` TEXT NOT NULL, `peerId` TEXT NOT NULL, `remotePeerId` TEXT NOT NULL, `remotePeerMeta` TEXT NOT NULL, `isAutoSign` INTEGER NOT NULL, `date` INTEGER NOT NULL, PRIMARY KEY(`remotePeerId`))")
            }
        }

        private val MIGRATION_27_28: Migration = object : Migration(27, 28) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // extract custom coins
                val customCoins = extractCustomCoins(database)
                customCoins.forEach {
                    App.coinKit.saveCoin(it)
                }

                // change coinIds in enabled wallets
                updateCoinIdInEnabledWallets(customCoins, database)

                //drop CoinRecord table and clean PriceAlert table
                database.execSQL("DROP TABLE CoinRecord")
            }
        }

        private val MIGRATION_28_29: Migration = object : Migration(28, 29) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE FavoriteCoin")
                database.execSQL("CREATE TABLE IF NOT EXISTS `FavoriteCoin` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `coinType` TEXT NOT NULL)")
            }
        }

        private val MIGRATION_29_30: Migration = object : Migration(29, 30) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE SubscriptionJob")
                database.execSQL("CREATE TABLE IF NOT EXISTS SubscriptionJob (`coinCode` TEXT NOT NULL, `topicName` TEXT NOT NULL, `stateType` TEXT NOT NULL, `jobType` TEXT NOT NULL, PRIMARY KEY(`coinCode`, `stateType`))")

                //unsubscribe from old Notifications
                addNotificationsUnsubscribeJobs(database)

                database.execSQL("DROP TABLE PriceAlert")
                database.execSQL("CREATE TABLE IF NOT EXISTS PriceAlert (`coinType` TEXT NOT NULL, `notificationCoinCode` TEXT NOT NULL, `coinName` TEXT NOT NULL, `changeState` TEXT NOT NULL, `trendState` TEXT NOT NULL, PRIMARY KEY(`coinType`))")
            }
        }

        private fun addNotificationsUnsubscribeJobs(database: SupportSQLiteDatabase) {
            val unsubscribeJobs = mutableListOf<SubscriptionJob>()
            val priceAlertsCursor = database.query("SELECT * FROM PriceAlert")
            while (priceAlertsCursor.moveToNext()) {
                val coinIdColumn = priceAlertsCursor.getColumnIndex("coinId")
                if (coinIdColumn >= 0) {
                    val coinId = priceAlertsCursor.getString(coinIdColumn)
                    val changeColumn = priceAlertsCursor.getColumnIndex("changeState")
                    if (changeColumn >= 0) {
                        val changeValue = priceAlertsCursor.getString(changeColumn)
                        if (changeValue != PriceAlert.ChangeState.OFF.value) {
                            unsubscribeJobs.add(PriceAlertManager.getChangeSubscriptionJob(coinId, changeValue, SubscriptionJob.JobType.Unsubscribe))
                        }
                    }
                    val trendColumn = priceAlertsCursor.getColumnIndex("trendState")
                    if (trendColumn >= 0) {
                        val trendValue = priceAlertsCursor.getString(trendColumn)
                        if (trendValue != PriceAlert.TrendState.OFF.value) {
                            unsubscribeJobs.add(PriceAlertManager.getTrendSubscriptionJob(coinId, trendValue, SubscriptionJob.JobType.Unsubscribe))
                        }
                    }
                }
            }

            unsubscribeJobs.forEach { job ->
                database.execSQL("""
                                        INSERT INTO SubscriptionJob (`coinCode`,`topicName`,`stateType`,`jobType`) 
                                        VALUES ('${job.coinCode}', '${job.topicName}', '${job.stateType.value}', '${job.jobType.value}')
                                        """.trimIndent())
            }
        }

        private fun extractCustomCoins(database: SupportSQLiteDatabase): List<Coin> {
            val coins = mutableListOf<Coin>()
            val coinRecordCursor = database.query("SELECT * FROM CoinRecord")
            while (coinRecordCursor.moveToNext()) {
                var title = ""
                var code = ""
                var decimal = 0

                val titleColumn = coinRecordCursor.getColumnIndex("title")
                if (titleColumn >= 0) {
                    title = coinRecordCursor.getString(titleColumn)
                }
                val codeColumn = coinRecordCursor.getColumnIndex("code")
                if (codeColumn >= 0) {
                    code = coinRecordCursor.getString(codeColumn)
                }
                val decimalColumn = coinRecordCursor.getColumnIndex("decimal")
                if (decimalColumn >= 0) {
                    decimal = coinRecordCursor.getInt(decimalColumn)
                }

                val erc20AddressColumn = coinRecordCursor.getColumnIndex("erc20Address")
                if (erc20AddressColumn >= 0) {
                    val erc20Address = coinRecordCursor.getString(erc20AddressColumn)
                    if (erc20Address.isNotBlank()) {
                        val coin = Coin(CoinType.Erc20(erc20Address), code, title, decimal)
                        coins.add(coin)
                        continue
                    }
                }
                val bep2SymbolColumn = coinRecordCursor.getColumnIndex("bep2Symbol")
                if (bep2SymbolColumn >= 0) {
                    val bep2Symbol = coinRecordCursor.getString(bep2SymbolColumn)
                    if (bep2Symbol.isNotBlank()) {
                        val coin = Coin(CoinType.Bep2(bep2Symbol), code, title, decimal)
                        coins.add(coin)
                    }
                }
            }
            return coins
        }

        private fun updateCoinIdInEnabledWallets(customCoins: List<Coin>, database: SupportSQLiteDatabase) {
            val allCoins = App.coinKit.getDefaultCoins() + customCoins
            val walletsCursor = database.query("SELECT * FROM EnabledWallet")
            while (walletsCursor.moveToNext()) {
                val coinIdColumnIndex = walletsCursor.getColumnIndex("coinId")
                var oldCoinId = ""
                if (coinIdColumnIndex >= 0) {
                    oldCoinId = walletsCursor.getString(coinIdColumnIndex)
                }
                var accountId = ""
                val accountIdColumnIndex = walletsCursor.getColumnIndex("accountId")
                if (accountIdColumnIndex >= 0) {
                    accountId = walletsCursor.getString(accountIdColumnIndex)
                }

                if (oldCoinId.isEmpty() || accountId.isEmpty()){
                    continue
                }

                newCoinId(oldCoinId, allCoins)?.let { newCoinId ->
                    database.execSQL("""
                        UPDATE EnabledWallet 
                        SET coinId = '$newCoinId' 
                        WHERE coinId = '$oldCoinId' AND accountId = '$accountId';
                    """.trimIndent())
                }
            }
        }

        private fun newCoinId(old: String, coins: List<Coin>): String? {
            oldTypeIds[old]?.let {
                return it.ID
            }

            coins.firstOrNull { it.code == old }?.let {
                return it.id
            }

            coins.firstOrNull { coin ->
                (coin.type as? CoinType.Bep2)?.symbol == old
            }?.let {
                return it.id
            }

            return null
        }

        private val oldTypeIds: Map<String, CoinType> = mapOf(
                "BNB-ERC20" to CoinType.Erc20("0xb8c77482e45f1f44de1745f52c74426c631bdd52"),
                "BNB" to CoinType.Bep2("BNB"),
                "BNB-BSC" to CoinType.BinanceSmartChain,
                "DOS" to CoinType.Bep2("DOS-120"),
                "DOS-ERC20" to CoinType.Erc20("0x0a913bead80f321e7ac35285ee10d9d922659cb7"),
                "ETH" to CoinType.Ethereum,
                "ETH-BEP2" to CoinType.Bep2("ETH-1c9"),
                "MATIC" to CoinType.Erc20("0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0"),
                "MATIC-BEP2" to CoinType.Bep2("MATIC-84a"),
                "AAVEDAI" to CoinType.Erc20("0xfc1e690f61efd961294b3e1ce3313fbd8aa4f85d"),
                "AMON" to CoinType.Erc20("0x737f98ac8ca59f2c68ad658e3c3d8c8963e40a4c"),
                "RENBTC" to CoinType.Erc20("0xeb4c2781e4eba804ce9a9803c67d0893436bb27d"),
                "RENBCH" to CoinType.Erc20("0x459086f2376525bdceba5bdda135e4e9d3fef5bf"),
                "RENZEC" to CoinType.Erc20("0x1c5db575e2ff833e46a2e9864c22f4b22e0b37c2"),
        )

    }
}
