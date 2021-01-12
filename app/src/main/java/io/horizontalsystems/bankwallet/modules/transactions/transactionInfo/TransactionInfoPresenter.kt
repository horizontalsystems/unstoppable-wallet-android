package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoModule.TitleViewItem
import java.util.*

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transaction: TransactionRecord,
        private val wallet: Wallet,
        private val transactionInfoAddressMapper: TransactionInfoAddressMapper
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

        viewItems.add(TransactionDetailViewItem.Status(status, transaction.type == TransactionType.Incoming))

        rate?.let {
            viewItems.add(TransactionDetailViewItem.Rate(rate, wallet.coin.code))
        }

        transaction.fee?.let { fee ->
            val feeCoin = interactor.feeCoin(coin) ?: coin
            viewItems.add(TransactionDetailViewItem.Fee(CoinValue(feeCoin, fee), rate?.let { CurrencyValue(it.currency, it.value * fee) }))
        }

        transaction.from?.let { from ->
            if (showFromAddress(wallet.coin.type)) {
                viewItems.add(TransactionDetailViewItem.From(transactionInfoAddressMapper.map(from)))
            }
        }

        transaction.to?.let { to ->
            viewItems.add(TransactionDetailViewItem.To(transactionInfoAddressMapper.map(to)))
        }

        transaction.memo?.let { memo ->
            viewItems.add(TransactionDetailViewItem.Memo(memo))
        }

        transaction.lockInfo?.originalAddress?.let { recipient ->
            if (transaction.type == TransactionType.Outgoing) {
                viewItems.add(TransactionDetailViewItem.Recipient(transactionInfoAddressMapper.map(recipient)))
            }
        }

        if (transaction.showRawTransaction) {
            viewItems.add(TransactionDetailViewItem.RawTransaction())
        } else {
            viewItems.add(TransactionDetailViewItem.Id(transaction.transactionHash))
        }

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
        view?.setExplorerButtonName(getExplorerName(coin.type))
    }

    private fun showFromAddress(coinType: CoinType): Boolean {
        return !(coinType == CoinType.Bitcoin || coinType == CoinType.BitcoinCash || coinType == CoinType.Dash)
    }


    override fun onShare() {
        view?.share(transaction.transactionHash)
    }

    override fun openExplorer() {
        val url = getFullUrl(transaction.transactionHash, interactor.testMode, wallet.coin.type) ?: return
        router.showTransactionInfoInExplorer(url)
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

    override fun onClickStatusInfo() {
        router.openStatusInfo()
    }

    private fun onCopy(value: String?) {
        value?.let {
            interactor.copyToClipboard(value)
            view?.showCopied()
        }
    }

    private fun getFullUrl(hash: String, testMode: Boolean, coinType: CoinType): String? {
        return when (coinType) {
            is CoinType.Bitcoin -> "https://btc.com/$hash"
            is CoinType.Litecoin -> "https://blockchair.com/litecoin/transaction/$hash"
            is CoinType.BitcoinCash -> "https://bch.btc.com/$hash"
            is CoinType.Dash -> "https://blockchair.com/dash/transaction/$hash"
            is CoinType.Binance -> "https://${if (testMode) "testnet-explorer" else "explorer"}.binance.org/tx/$hash"
            is CoinType.Eos -> null
            is CoinType.Zcash -> "https://explorer.zcha.in/transactions/$hash"
            else -> "https://${if (testMode) "ropsten." else ""}etherscan.io/tx/$hash" // ETH, ETHt
        }
    }

    private fun getExplorerName(coinType: CoinType): String {
        return when (coinType) {
            is CoinType.Bitcoin, is CoinType.BitcoinCash -> "btc.com"
            is CoinType.Litecoin, is CoinType.Dash -> "blockchair.com"
            is CoinType.Binance -> "binance.org"
            is CoinType.Eos -> ""
            is CoinType.Zcash -> "zcha.in"
            else -> "etherscan.io"
        }
    }
}
