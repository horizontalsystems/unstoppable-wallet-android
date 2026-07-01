package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_38_39 : Migration(38, 39) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `WalletConnectV2Session` (`accountId` TEXT NOT NULL, `topic` TEXT NOT NULL, PRIMARY KEY(`accountId`, `topic`))")
    }
}
