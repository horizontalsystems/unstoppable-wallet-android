package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_42_43 : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ProFeaturesSessionKey` (`nftName` TEXT NOT NULL, `accountId` TEXT NOT NULL, `address` TEXT NOT NULL, `key` TEXT NOT NULL, PRIMARY KEY(`nftName`, `accountId`))")
    }
}
