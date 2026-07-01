package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_31_32 : Migration(31, 32) {

    override fun migrate(db: SupportSQLiteDatabase) {
        createTableActiveAccount(db)
        createTableRestoreSettings(db)

        handleZcashAccount(db)
        updateAccountRecordTable(db)
        moveCoinSettingsFromBlockchainSettingsToWallet(db)
        setActiveAccount(db)
        setAccountUserFriendlyName(db)
    }

    private fun handleZcashAccount(database: SupportSQLiteDatabase) {
        val zcashAccountsCursor = database.query("SELECT * FROM AccountRecord WHERE type = 'zcash'")
        val idColumnIndex = zcashAccountsCursor.getColumnIndex("id")
        val birthdayHeightColumnIndex = zcashAccountsCursor.getColumnIndex("birthdayHeight")
        if (idColumnIndex >= 0 && birthdayHeightColumnIndex >= 0) {
            while (zcashAccountsCursor.moveToNext()) {
                val id = zcashAccountsCursor.getString(idColumnIndex)
                val birthdayHeight = zcashAccountsCursor.getString(birthdayHeightColumnIndex)

                database.execSQL("INSERT INTO `RestoreSettingRecord`(`accountId`, `coinId`, `key`, `value`) VALUES('$id', 'zcash', 'birthdayHeight', '$birthdayHeight')")
            }
        }
        database.execSQL("UPDATE `AccountRecord` SET `type` = 'mnemonic' WHERE `type` = 'zcash'")
    }

    private fun updateAccountRecordTable(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE AccountRecord RENAME TO TempAccountRecord")
        database.execSQL("CREATE TABLE IF NOT EXISTS `AccountRecord` (`deleted` INTEGER NOT NULL, `id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `origin` TEXT NOT NULL, `isBackedUp` INTEGER NOT NULL, `words` TEXT, `passphrase` TEXT, `key` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("INSERT INTO AccountRecord (`deleted`, `id`, `name`, `type`, `origin`, `isBackedUp`, `words`, `passphrase`, `key`) SELECT `deleted`, `id`, `name`, `type`, `origin`, `isBackedUp`, `words`, `salt`, `key` FROM TempAccountRecord")
        database.execSQL("DROP TABLE TempAccountRecord")
    }

    private fun createTableActiveAccount(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `ActiveAccount` (`accountId` TEXT NOT NULL, `primaryKey` TEXT NOT NULL, PRIMARY KEY(`primaryKey`))")
    }

    private fun createTableRestoreSettings(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `RestoreSettingRecord` (`accountId` TEXT NOT NULL, `coinId` TEXT NOT NULL, `key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`accountId`, `coinId`, `key`))")
    }

    private fun moveCoinSettingsFromBlockchainSettingsToWallet(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE EnabledWallet RENAME TO TempEnabledWallet")
        database.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWallet` (`coinId` TEXT NOT NULL, `coinSettingsId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `walletOrder` INTEGER, PRIMARY KEY(`coinId`, `coinSettingsId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        database.execSQL("INSERT INTO EnabledWallet (`coinId`, `coinSettingsId`, `accountId`, `walletOrder`) SELECT `coinId`,'',`accountId`,`walletOrder` FROM TempEnabledWallet")
        database.execSQL("DROP TABLE TempEnabledWallet")
        database.execSQL("CREATE INDEX `index_EnabledWallet_accountId` ON `EnabledWallet` (`accountId`)")

        val settingsToMigrateCursor = database.query("SELECT * FROM BlockchainSetting WHERE key IN('derivation', 'network_coin_type')")
        val coinTypeIndex = settingsToMigrateCursor.getColumnIndex("coinType")
        val keyIndex = settingsToMigrateCursor.getColumnIndex("key")
        val valueIndex = settingsToMigrateCursor.getColumnIndex("value")

        if (coinTypeIndex >= 0 && keyIndex >= 0 && valueIndex >= 0) {
            while (settingsToMigrateCursor.moveToNext()) {
                val coinType = settingsToMigrateCursor.getString(coinTypeIndex)
                val key = settingsToMigrateCursor.getString(keyIndex)
                val value = settingsToMigrateCursor.getString(valueIndex)

                val coinSettingsId = when (key) {
                    "derivation" -> "derivation:$value"
                    "network_coin_type" -> "bitcoinCashCoinType:$value"
                    else -> continue
                }

                database.execSQL("UPDATE `EnabledWallet` SET `coinSettingsId` = '$coinSettingsId' WHERE `coinId` = '$coinType'")
            }
        }

        database.execSQL("DELETE FROM `BlockchainSetting` WHERE `key` IN('derivation', 'network_coin_type')")
    }

    private fun setActiveAccount(database: SupportSQLiteDatabase) {
        val firstAccountCursor = database.query("SELECT id FROM AccountRecord ORDER BY rowid LIMIT 1")
        val idColumnIndex = firstAccountCursor.getColumnIndex("id")
        if (idColumnIndex >= 0 && firstAccountCursor.moveToNext()) {
            val id = firstAccountCursor.getString(idColumnIndex)

            database.execSQL("INSERT INTO `ActiveAccount`(`accountId`, `primaryKey`) VALUES('$id', 'active_account')")
        }
    }

    private fun setAccountUserFriendlyName(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `AccountRecord` SET `name` = 'Wallet ' || `rowid`")
    }
}
