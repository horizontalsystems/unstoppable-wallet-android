package io.horizontalsystems.walletkit.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_85_86 : Migration(85, 86) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `splitOutputs` TEXT")
    }
}
