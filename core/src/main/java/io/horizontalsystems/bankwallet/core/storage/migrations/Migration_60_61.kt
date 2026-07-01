package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_60_61 : Migration(60, 61) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamAddress` (`transactionHash` BLOB NOT NULL, `address` TEXT NOT NULL, `domain` TEXT, `blockchainType` TEXT, PRIMARY KEY(`transactionHash`, `address`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamScanState` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `lastTransactionHash` BLOB NOT NULL, PRIMARY KEY(`blockchainType`, `accountId`))")
    }
}
