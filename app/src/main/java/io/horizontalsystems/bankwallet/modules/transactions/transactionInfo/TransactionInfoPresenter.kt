package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoModule.TitleViewItem
import java.util.*

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transaction: TransactionRecord,
        private val wallet: Wallet
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null

    // IViewDelegate methods

    override fun viewDidLoad() {
        val coin = wallet.coin
        val lastBlockInfo = interactor.lastBlockInfo

        val status = transaction.status(lastBlockInfo?.height)
        val lockState = transaction.lockState(lastBlockInfo?.timestamp)

        val rate = interactor.getRate(wallet.coin.code, transaction.timestamp)

        val primaryAmountInfo: SendModule.AmountInfo
        val secondaryAmountInfo: SendModule.AmountInfo?

        val coinValue = CoinValue(coin, transaction.amount)
        if (rate != null) {
            primaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * transaction.amount))
            secondaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        } else {
            primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            secondaryAmountInfo = null
        }

        val date = if (transaction.timestamp == 0L) null else Date(transaction.timestamp * 1000)

        view?.showTitle(TitleViewItem(date, primaryAmountInfo, secondaryAmountInfo, transaction.type, lockState))

        val viewItems = mutableListOf<TransactionDetailViewItem>()

        rate?.let {
            viewItems.add(TransactionDetailViewItem.Rate(rate, wallet.coin.code))
        }

        transaction.fee?.let { fee ->
            val feeCoin = interactor.feeCoin(coin) ?: coin
            viewItems.add(TransactionDetailViewItem.Fee(CoinValue(feeCoin, fee), rate?.let { CurrencyValue(it.currency, it.value * fee) }))
        }

        transaction.from?.let { from ->
            if (showFromAddress(wallet.coin.type)) {
                viewItems.add(TransactionDetailViewItem.From(from))
            }
        }

        transaction.to?.let { to ->
            viewItems.add(TransactionDetailViewItem.To(to))
        }

        transaction.lockInfo?.originalAddress?.let { recipient ->
            if (transaction.type == TransactionType.Outgoing) {
                viewItems.add(TransactionDetailViewItem.Recipient(recipient))
            }
        }

        if (transaction.showRawTransaction) {
            viewItems.add(TransactionDetailViewItem.RawTransaction())
        } else {
            viewItems.add(TransactionDetailViewItem.Id(transaction.transactionHash))
        }

        viewItems.add(TransactionDetailViewItem.Status(status, transaction.type == TransactionType.Incoming))

        if (transaction.conflictingTxHash != null) {
            viewItems.add(TransactionDetailViewItem.DoubleSpend())
        }

        lockState?.let {
            viewItems.add(TransactionDetailViewItem.LockInfo(it))
        }

        if (transaction.type == TransactionType.SentToSelf) {
            viewItems.add(TransactionDetailViewItem.SentToSelf())
        }

        view?.showDetails(viewItems)
    }

    private fun showFromAddress(coinType: CoinType): Boolean {
        return !(coinType == CoinType.Bitcoin || coinType == CoinType.BitcoinCash || coinType == CoinType.Dash)
    }


    override fun onShare() {
        view?.share(transaction.transactionHash)
    }

    override fun openFullInfo() {
        router.openFullInfo(transaction.transactionHash, wallet)
    }

    override fun onClickLockInfo() {
        transaction.lockInfo?.lockedUntil?.let {
            router.openLockInfo(it)
        }
    }

    override fun onClickDoubleSpendInfo() {
        transaction.conflictingTxHash?.let { conflictingTxHash ->
            router.openDoubleSpendInfo(transaction.transactionHash, conflictingTxHash)
        }
    }

    override fun onClickRecipientHash() {
        onCopy(transaction.lockInfo?.originalAddress)
    }

    override fun onClickTo() {
        onCopy(transaction.to)
    }

    override fun onClickFrom() {
        onCopy(transaction.from)
    }

    override fun onClickTransactionId() {
        onCopy(transaction.transactionHash)
    }

    override fun onRawTransaction() {
        onCopy(interactor.getRaw(transaction.transactionHash))
    }

    private fun onCopy(value: String?) {
        value?.let {
            interactor.copyToClipboard(value)
            view?.showCopied()
        }
    }
}
