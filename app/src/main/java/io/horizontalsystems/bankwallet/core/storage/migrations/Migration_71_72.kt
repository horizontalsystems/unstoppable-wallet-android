package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_71_72 : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `SwapRecord` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `providerId` TEXT NOT NULL,
                `providerName` TEXT NOT NULL,
                `tokenInUid` TEXT NOT NULL,
                `tokenInCoinCode` TEXT NOT NULL,
                `tokenInCoinUid` TEXT NOT NULL,
                `tokenInBadge` TEXT,
                `tokenInBlockchainTypeUid` TEXT NOT NULL,
                `tokenOutUid` TEXT NOT NULL,
                `tokenOutCoinCode` TEXT NOT NULL,
                `tokenOutCoinUid` TEXT NOT NULL,
                `tokenOutBadge` TEXT,
                `tokenOutBlockchainTypeUid` TEXT NOT NULL,
                `amountIn` TEXT NOT NULL,
                `amountOut` TEXT,
                `amountOutMin` TEXT,
                `recipientAddress` TEXT,
                `sourceAddress` TEXT,
                `transactionHash` TEXT,
                `slippage` TEXT,
                `networkFeeCoinCode` TEXT,
                `networkFeeAmount` TEXT,
                `providerSwapId` TEXT,
                `status` TEXT NOT NULL DEFAULT 'Depositing',
                `fromAsset` TEXT,
                `toAsset` TEXT,
                `depositAddress` TEXT
            )
        """.trimIndent())
    }
}
