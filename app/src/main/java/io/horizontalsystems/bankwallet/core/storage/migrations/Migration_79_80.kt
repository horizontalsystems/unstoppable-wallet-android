package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_79_80 : Migration(79, 80) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `OcpPaymentRecord` (
                `txHash` TEXT NOT NULL,
                `paymentId` TEXT NOT NULL,
                `quoteId` TEXT NOT NULL,
                `proofUrl` TEXT NOT NULL,
                `method` TEXT NOT NULL,
                `merchant` TEXT,
                `expirationIso` TEXT,
                `createdAt` INTEGER NOT NULL,
                `proofSubmittedAt` INTEGER,
                `proofFailedAt` INTEGER,
                PRIMARY KEY(`txHash`)
            )
        """.trimIndent())
    }
}
