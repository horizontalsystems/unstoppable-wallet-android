package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.core.factories.TransactionViewItemFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoModule.TitleViewItem
import java.util.*

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transactionHash: String,
        private val wallet: Wallet,
        private val transactionViewItemFactory: TransactionViewItemFactory
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null

    private lateinit var transactionRecord: TransactionRecord

    // IViewDelegate methods

    override fun viewDidLoad() {
        transactionRecord = interactor.getTransactionRecord(wallet, transactionHash) ?: throw IllegalStateException("Transaction Not Found")

        val threshold = interactor.getThreshold(wallet)
        val lastBlockInfo = interactor.getLastBlockInfo(wallet)
        val rate = interactor.getRate(wallet.coin.code, transactionRecord.timestamp)

        val item = transactionViewItemFactory.item(wallet, transactionRecord, lastBlockInfo, threshold, rate)

        val coin = wallet.coin

        val primaryAmountInfo: SendModule.AmountInfo
        val secondaryAmountInfo: SendModule.AmountInfo?

        val coinValue = CoinValue(coin, transactionRecord.amount)
        if (rate != null) {
            primaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * transactionRecord.amount))
            secondaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        } else {
            primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            secondaryAmountInfo = null
        }

        val date = if (transactionRecord.timestamp == 0L) null else Date(transactionRecord.timestamp * 1000)

        view?.showTitle(TitleViewItem(date, primaryAmountInfo, secondaryAmountInfo, transactionRecord.type,true))

        view?.showTransaction(item)
    }

    override fun onShare() {
        view?.share(transactionRecord.transactionHash)
    }

    override fun openFullInfo() {
        router.openFullInfo(transactionRecord.transactionHash, wallet)
    }

    override fun onClickLockInfo() {
        transactionRecord.lockInfo?.lockedUntil?.let {
            router.openLockInfo(it)
        }
    }

    override fun onClickDoubleSpendInfo() {
        transactionRecord.conflictingTxHash?.let { conflictingTxHash ->
            router.openDoubleSpendInfo(transactionRecord.transactionHash, conflictingTxHash)
        }
    }

    override fun onClickRecipientHash() {
        onCopy(transactionRecord.lockInfo?.originalAddress)
    }

    override fun onClickTo() {
        onCopy(transactionRecord.to)
    }

    override fun onClickFrom() {
        onCopy(transactionRecord.from)
    }

    override fun onClickTransactionId() {
        onCopy(transactionRecord.transactionHash)
    }

    private fun onCopy(value: String?) {
        value?.let {
            interactor.onCopy(value)
            view?.showCopied()
        }
    }
}
