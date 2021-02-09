package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import java.util.*

sealed class TransactionDetailViewItem {
    class Rate(val currencyValue: CurrencyValue, val coinCode: String) : TransactionDetailViewItem()
    class Fee(val coinValue: CoinValue, val currencyValue: CurrencyValue?) : TransactionDetailViewItem()
    class From(val from: String) : TransactionDetailViewItem()
    class Memo(val memo: String) : TransactionDetailViewItem()
    class To(val to: String) : TransactionDetailViewItem()
    class Recipient(val recipient: String) : TransactionDetailViewItem()
    class Id(val id: String) : TransactionDetailViewItem()
    class Status(val status: TransactionStatus, val incoming: Boolean) : TransactionDetailViewItem()
    class DoubleSpend : TransactionDetailViewItem()
    class SentToSelf : TransactionDetailViewItem()
    class RawTransaction : TransactionDetailViewItem()
    class LockInfo(val lockState: TransactionLockState) : TransactionDetailViewItem()
}

data class TransactionLockState(val locked: Boolean, val date: Date)

