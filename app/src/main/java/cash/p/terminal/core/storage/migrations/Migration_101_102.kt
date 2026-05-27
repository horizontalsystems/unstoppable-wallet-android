package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("ClassName")
object Migration_101_102 : Migration(101, 102) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS LocallyCreatedTransaction (
                accountId TEXT NOT NULL,
                blockchainTypeUid TEXT NOT NULL,
                transactionHash TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY (accountId, blockchainTypeUid, transactionHash),
                FOREIGN KEY (accountId) REFERENCES AccountRecord(id)
                    ON UPDATE CASCADE ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_LocallyCreatedTransaction_accountId_createdAt
            ON LocallyCreatedTransaction(accountId, createdAt)
            """.trimIndent()
        )
    }
}
