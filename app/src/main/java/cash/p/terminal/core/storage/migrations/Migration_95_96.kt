package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_95_96 : Migration(95, 96) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val cursor = db.query("PRAGMA table_info(PendingMultiSwap)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        if ("completedAt" !in columns) return

        val keepColumns = listOf(
            "id", "createdAt", "coinUidIn", "blockchainTypeIn", "amountIn",
            "coinUidIntermediate", "blockchainTypeIntermediate", "coinUidOut", "blockchainTypeOut",
            "leg1ProviderId", "leg1IsOffChain", "leg1TransactionId", "leg1AmountOut", "leg1Status",
            "leg2ProviderId", "leg2IsOffChain", "leg2TransactionId", "leg2AmountOut", "leg2Status",
            "expectedAmountOut", "leg2StartedAt",
            "leg1ProviderTransactionId", "leg2ProviderTransactionId", "leg1InfoRecordUid"
        )
        val columnList = keepColumns.joinToString(", ")

        db.execSQL("""
            CREATE TABLE PendingMultiSwap_new (
                id TEXT NOT NULL PRIMARY KEY,
                createdAt INTEGER NOT NULL,
                coinUidIn TEXT NOT NULL,
                blockchainTypeIn TEXT NOT NULL,
                amountIn TEXT NOT NULL,
                coinUidIntermediate TEXT NOT NULL,
                blockchainTypeIntermediate TEXT NOT NULL,
                coinUidOut TEXT NOT NULL,
                blockchainTypeOut TEXT NOT NULL,
                leg1ProviderId TEXT NOT NULL,
                leg1IsOffChain INTEGER NOT NULL,
                leg1TransactionId TEXT,
                leg1AmountOut TEXT,
                leg1Status TEXT NOT NULL,
                leg2ProviderId TEXT,
                leg2IsOffChain INTEGER,
                leg2TransactionId TEXT,
                leg2AmountOut TEXT,
                leg2Status TEXT NOT NULL,
                expectedAmountOut TEXT NOT NULL,
                leg2StartedAt INTEGER,
                leg1ProviderTransactionId TEXT,
                leg2ProviderTransactionId TEXT,
                leg1InfoRecordUid TEXT
            )
        """.trimIndent())
        db.execSQL("INSERT INTO PendingMultiSwap_new ($columnList) SELECT $columnList FROM PendingMultiSwap")
        db.execSQL("DROP TABLE PendingMultiSwap")
        db.execSQL("ALTER TABLE PendingMultiSwap_new RENAME TO PendingMultiSwap")
    }
}
