package io.horizontalsystems.walletkit.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_83_84 : Migration(83, 84) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `estimatedTime` INTEGER")
    }
}
