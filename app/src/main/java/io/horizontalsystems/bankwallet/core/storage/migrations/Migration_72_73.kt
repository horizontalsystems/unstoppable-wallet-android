package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_72_73 : Migration(72, 73) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `fromAsset` TEXT")
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `toAsset` TEXT")
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `depositAddress` TEXT")
    }
}
