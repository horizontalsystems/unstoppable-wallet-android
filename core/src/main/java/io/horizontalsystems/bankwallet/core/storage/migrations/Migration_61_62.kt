package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_61_62 : Migration(61, 62) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `RecentAddress` (`accountId` TEXT NOT NULL, `blockchainType` TEXT NOT NULL, `address` TEXT NOT NULL, PRIMARY KEY(`accountId`, `blockchainType`))")
    }
}
