package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_72_73 : Migration(72, 73) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS RecentAddress")
        db.execSQL("DROP TABLE IF EXISTS SpamAddress")
        db.execSQL("DROP TABLE IF EXISTS SpamScanState")

        db.execSQL(
            """
            CREATE TABLE RecentAddress (
                accountId TEXT NOT NULL,
                address TEXT NOT NULL,
                blockchainType TEXT NOT NULL,
                PRIMARY KEY(accountId, blockchainType)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE SpamAddress (
                transactionHash BLOB NOT NULL,
                address TEXT NOT NULL,
                domain TEXT,
                blockchainType TEXT,
                PRIMARY KEY(transactionHash, address)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE SpamScanState (
                blockchainType TEXT NOT NULL,
                accountId TEXT NOT NULL,
                lastTransactionHash BLOB NOT NULL,
                PRIMARY KEY(blockchainType, accountId)
            )
        """.trimIndent()
        )
    }
}
