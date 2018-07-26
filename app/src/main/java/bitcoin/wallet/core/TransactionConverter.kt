package bitcoin.wallet.core

import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem
import java.util.*
import kotlin.math.max

class TransactionConverter {

    fun convertToTransactionRecordViewItem(coin: Coin, transactionRecord: TransactionRecord, latestBlockHeight: Long, exchangeRate: Double?): TransactionRecordViewItem {

        val confirmations =
                if (transactionRecord.blockHeight == 0L) {
                    0
                } else {
                    max(0, latestBlockHeight - transactionRecord.blockHeight + 1)
                }

        val valueInBaseCurrency = exchangeRate?.times(Math.abs(transactionRecord.amount / 100000000.0))
                ?: 0.0

        return TransactionRecordViewItem(
                transactionRecord.transactionHash,
                CoinValue(coin, transactionRecord.amount / 100000000.0),
                CoinValue(coin, transactionRecord.fee / 100000000.0),
                transactionRecord.from,
                transactionRecord.to,
                transactionRecord.incoming,
                transactionRecord.blockHeight,
                Date(transactionRecord.timestamp),
                if (confirmations > 0) TransactionRecordViewItem.Status.SUCCESS else TransactionRecordViewItem.Status.PENDING,
                confirmations,
                valueInBaseCurrency,
                exchangeRate
        )
    }

}
