package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_34_35 : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `EnabledWalletCache` (`coinId` TEXT NOT NULL, `coinSettingsId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `balance` TEXT NOT NULL, `balanceLocked` TEXT NOT NULL, PRIMARY KEY(`coinId`, `coinSettingsId`, `accountId`), FOREIGN KEY(`accountId`) REFERENCES `AccountRecord`(`id`) ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED)")
    }
}
