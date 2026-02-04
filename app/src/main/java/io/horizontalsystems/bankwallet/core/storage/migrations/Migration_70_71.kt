package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_70_71 : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop old SpamAddress table
        db.execSQL("DROP TABLE IF EXISTS `SpamAddress`")

        // Create new ScannedTransaction table with spamScore
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `ScannedTransaction` (
                `transactionHash` BLOB NOT NULL,
                `spamScore` INTEGER NOT NULL,
                `blockchainType` TEXT NOT NULL,
                `address` TEXT,
                PRIMARY KEY(`transactionHash`)
            )
        """.trimIndent())

        // Clear SpamScanState to trigger rescan of all transactions
        db.execSQL("DELETE FROM SpamScanState")
    }
}