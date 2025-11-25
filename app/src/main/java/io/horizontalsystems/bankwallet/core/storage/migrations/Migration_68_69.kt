package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_68_69 : Migration(68, 69) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM `EnabledWalletCache` WHERE 1")
        db.execSQL("ALTER TABLE `EnabledWalletCache` DROP COLUMN `balance`")
        db.execSQL("ALTER TABLE `EnabledWalletCache` DROP COLUMN `balanceLocked`")
        db.execSQL("ALTER TABLE `EnabledWalletCache` ADD COLUMN `balanceData` TEXT NULL")
    }
}
