package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_52_53 : Migration(52, 53) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `CexAssetRaw` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `name` TEXT NOT NULL, `freeBalance` TEXT NOT NULL, `lockedBalance` TEXT NOT NULL, `depositEnabled` INTEGER NOT NULL, `withdrawEnabled` INTEGER NOT NULL, `depositNetworks` TEXT NOT NULL, `withdrawNetworks` TEXT NOT NULL, `coinUid` TEXT, `decimals` INTEGER NOT NULL, PRIMARY KEY(`id`, `accountId`))")
    }
}