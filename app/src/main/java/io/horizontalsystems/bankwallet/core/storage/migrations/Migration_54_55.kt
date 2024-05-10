package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_54_55 : Migration(54, 55) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val derivations = listOf("bip44", "bip49", "bip84", "bip86")
        val blockchainTypes = listOf("bitcoin", "litecoin")

        blockchainTypes.forEach { blockchainTypeId ->
            derivations.forEach { derivation ->
                val tokenQueryId = "$blockchainTypeId|native"
                val coinSettingsId = "derivation:$derivation"
                val newTokenQueryId = "$blockchainTypeId|derived:${derivation.replaceFirstChar(Char::titlecase)}"

                db.execSQL("UPDATE `EnabledWallet` SET tokenQueryId = '$newTokenQueryId' WHERE tokenQueryId = '$tokenQueryId' AND coinSettingsId = '$coinSettingsId'")
            }
        }

        val bchTypes = listOf("type0", "type145")
        bchTypes.forEach { bchType ->
            val tokenQueryId = "bitcoin-cash|native"
            val coinSettingsId = "bitcoinCashCoinType:$bchType"
            val newTokenQueryId = "bitcoin-cash|address_type:${bchType.replaceFirstChar(Char::titlecase)}"

            db.execSQL("UPDATE `EnabledWallet` SET tokenQueryId = '$newTokenQueryId' WHERE tokenQueryId = '$tokenQueryId' AND coinSettingsId = '$coinSettingsId'")
        }



        db.execSQL("ALTER TABLE EnabledWallet RENAME TO TempEnabledWallet")
        db.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWallet` (`tokenQueryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `walletOrder` INTEGER, `coinName` TEXT, `coinCode` TEXT, `coinDecimals` INTEGER, PRIMARY KEY(`tokenQueryId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
        db.execSQL("INSERT INTO `EnabledWallet`(tokenQueryId, accountId, walletOrder, coinName, coinCode, coinDecimals) SELECT tokenQueryId, accountId, walletOrder, coinName, coinCode, coinDecimals FROM `TempEnabledWallet`")
        db.execSQL("DROP TABLE IF EXISTS `TempEnabledWallet`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_EnabledWallet_accountId` ON `EnabledWallet` (`accountId`)")

        db.execSQL("DROP TABLE IF EXISTS `EnabledWalletCache`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWalletCache` (`tokenQueryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `balance` TEXT NOT NULL, `balanceLocked` TEXT NOT NULL, PRIMARY KEY(`tokenQueryId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")

    }
}