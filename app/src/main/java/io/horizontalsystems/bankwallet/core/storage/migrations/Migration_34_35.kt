package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_34_35 : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE EnabledWallet ADD COLUMN `balance` TEXT NOT NULL DEFAULT '0'")
        database.execSQL("ALTER TABLE EnabledWallet ADD COLUMN `balanceLocked` TEXT NOT NULL DEFAULT '0'")
    }
}
