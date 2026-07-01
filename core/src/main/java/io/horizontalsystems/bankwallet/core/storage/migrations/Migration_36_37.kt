package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_36_37 : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `EvmAccountState` (`accountId` TEXT NOT NULL, `chainId` INTEGER NOT NULL, `transactionsSyncedBlockNumber` INTEGER NOT NULL, PRIMARY KEY(`accountId`, `chainId`))")
    }
}
