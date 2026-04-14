package com.quantum.wallet.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_75_76 : Migration(75, 76) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `customRecipientAddress` INTEGER NOT NULL DEFAULT 0")
    }
}
