package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_49_50 : Migration(49, 50) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `EvmSyncSourceRecord` (`url` TEXT NOT NULL, `blockchainTypeUid` TEXT NOT NULL, `auth` TEXT, PRIMARY KEY(`blockchainTypeUid`,`url`))")    }
}