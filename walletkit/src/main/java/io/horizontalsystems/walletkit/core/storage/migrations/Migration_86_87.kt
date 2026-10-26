package io.horizontalsystems.walletkit.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_86_87 : Migration(86, 87) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `splitOutputs` TEXT")
    }
}
