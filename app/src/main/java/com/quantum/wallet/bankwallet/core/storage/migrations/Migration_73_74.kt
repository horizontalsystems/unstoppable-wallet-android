package com.quantum.wallet.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_73_74 : Migration(73, 74) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `outboundTransactionHash` TEXT")
    }
}
