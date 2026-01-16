package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_69_70 : Migration(69, 70) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `USwapAssetRecord` (
                `providerId` TEXT NOT NULL,
                `tokenQueryId` TEXT NOT NULL,
                `assetIdentifier` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`providerId`, `tokenQueryId`)
            )
        """.trimIndent())
    }
}
