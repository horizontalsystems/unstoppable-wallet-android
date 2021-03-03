package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoModule.TitleViewItem
import io.horizontalsystems.coinkit.models.CoinType
import java.util.*

class TransactionInfoPresenter(
        private val interactor: TransactionInfoModule.Interactor,
        private val router: TransactionInfoModule.Router,
        private val transaction: TransactionRecord,
        private val wallet: Wallet,
        private val transactionInfoAddressMapper: TransactionInfoAddressMapper
) : TransactionInfoModule.ViewDelegate, TransactionInfoModule.InteractorDelegate {

    var view: TransactionInfoModule.View? = null
    private var explorerData: TransactionInfoModule.ExplorerData = getExplorerData(transaction.transactionHash, interactor.testMode, wallet.coin.type)

    // IViewDelegate methods

    override fun viewDidLoad() {
        val coin = wallet.coin
        val lastBlockInfo = interactor.lastBlockInfo

        val status = transaction.status(lastBlockInfo?.height)
        val lockState = transaction.lockState(lastBlockInfo?.timestamp)

        val rate = interactor.getRate(wallet.coin.type, transaction.timestamp)

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

        view?.setExplorerButton(explorerData.title, explorerData.url != null)
    }

    private fun showFromAddress(coinType: CoinType): Boolean {
        return !(coinType == CoinType.Bitcoin || coinType == CoinType.BitcoinCash || coinType == CoinType.Dash)
    }


    override fun onShare() {
        view?.share(transaction.transactionHash)
    }

    override fun openExplorer() {
        val url = explorerData.url ?: return
        router.openUrl(url)
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

    private fun getExplorerData(hash: String, testMode: Boolean, coinType: CoinType): TransactionInfoModule.ExplorerData {
        return when (coinType) {
            is CoinType.Bitcoin -> TransactionInfoModule.ExplorerData("blockchair.com", if (testMode) null else "https://blockchair.com/bitcoin/transaction/$hash")
            is CoinType.BitcoinCash -> TransactionInfoModule.ExplorerData("btc.com", if (testMode) null else "https://bch.btc.com/$hash")
            is CoinType.Litecoin -> TransactionInfoModule.ExplorerData("blockchair.com", if (testMode) null else "https://blockchair.com/litecoin/transaction/$hash")
            is CoinType.Dash -> TransactionInfoModule.ExplorerData("dash.org", if (testMode) null else "https://insight.dash.org/insight/tx/$hash")
            is CoinType.Ethereum,
            is CoinType.Erc20 -> TransactionInfoModule.ExplorerData("etherscan.io", if (testMode) "https://ropsten.etherscan.io/tx/$hash" else "https://etherscan.io/tx/$hash")
            is CoinType.Bep2 -> TransactionInfoModule.ExplorerData("binance.org", if (testMode) "https://testnet-explorer.binance.org/tx/$hash" else "https://explorer.binance.org/tx/$hash")
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> TransactionInfoModule.ExplorerData("bscscan.com", if (testMode) null else "https://bscscan.com/tx/$hash")
            is CoinType.Zcash -> TransactionInfoModule.ExplorerData("blockchair.com", if (testMode) null else "https://blockchair.com/zcash/transaction/$hash")
            is CoinType.Unsupported ->  throw IllegalArgumentException()
        }
    }

}
