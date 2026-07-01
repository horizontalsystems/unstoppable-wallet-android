package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_64_65 : Migration(64, 65) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // recreate SpamScanState
        db.execSQL("DROP TABLE IF EXISTS `SpamScanState`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `SpamScanState` (`blockchainType` TEXT NOT NULL, `accountId` TEXT NOT NULL, `lastSyncedTransactionId` TEXT NOT NULL, PRIMARY KEY(`blockchainType`, `accountId`))")
    }
}