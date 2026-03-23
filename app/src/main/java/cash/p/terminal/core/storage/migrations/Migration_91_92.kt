package cash.p.terminal.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_91_92 : Migration(91, 92) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS PendingMultiSwap (
                id TEXT PRIMARY KEY NOT NULL,
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
                expectedAmountOut TEXT NOT NULL DEFAULT '0'
            )
            """
        )
    }
}
