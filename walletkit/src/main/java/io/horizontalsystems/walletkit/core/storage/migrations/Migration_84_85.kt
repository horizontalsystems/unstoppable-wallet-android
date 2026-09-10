package io.horizontalsystems.walletkit.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_84_85 : Migration(84, 85) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `SwapRecord` ADD COLUMN `operation` TEXT NOT NULL DEFAULT 'Swap'")
        // Every confidential-rail record written so far is a private send — the rail was
        // used for nothing else before this column existed. CrossPay records only start
        // appearing after this migration, so no other backfill is possible or needed.
        db.execSQL("UPDATE `SwapRecord` SET `operation` = 'PrivateSend' WHERE `providerId` = 'u_NEAR_CONFIDENTIAL'")
    }
}
