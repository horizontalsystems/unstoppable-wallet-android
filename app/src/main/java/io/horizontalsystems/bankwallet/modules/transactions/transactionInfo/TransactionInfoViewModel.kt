package io.horizontalsystems.bankwallet.modules.transactions.transactionInfo

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionType
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.ethereumkit.core.EthereumKit
import java.math.BigDecimal
import java.util.*

class TransactionInfoViewModel(
    private val service: TransactionInfoService,
    private val transaction: TransactionRecord,
    private val wallet: Wallet,
    private val transactionInfoAddressMapper: TransactionInfoAddressMapper
) : ViewModel() {

    val titleLiveData = MutableLiveData<TransactionInfoModule.TitleViewItem>()
    val detailsLiveData = SingleLiveEvent<List<TransactionDetailViewItem>>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()
    val showLockInfo = SingleLiveEvent<Date>()
    val showDoubleSpendInfo = SingleLiveEvent<Pair<String, String>>()
    val showShareLiveEvent = SingleLiveEvent<String>()
    val showStatusInfoLiveEvent = SingleLiveEvent<Unit>()
    val showTransactionLiveEvent = SingleLiveEvent<String>()
    val explorerButton = MutableLiveData<Pair<String, Boolean>>()

    private var explorerData: TransactionInfoModule.ExplorerData =
        getExplorerData(transaction.transactionHash, service.testMode, wallet.coin.type)

    init {
        val coin = wallet.coin
        val lastBlockInfo = service.lastBlockInfo

        val status = transaction.status(lastBlockInfo?.height)
        val lockState: TransactionLockState? = null
        val transactionType = transaction.getType(lastBlockInfo)

        val rate = service.getRate(wallet.coin.type, transaction.timestamp)

        val primaryAmountInfo: SendModule.AmountInfo
        val secondaryAmountInfo: SendModule.AmountInfo?

        val amount = transaction.mainValue?.value ?: BigDecimal.ZERO
        val coinValue = CoinValue(coin, transaction.mainValue?.value ?: BigDecimal.ZERO)
        if (rate != null) {
            primaryAmountInfo = SendModule.AmountInfo.CurrencyValueInfo(
                CurrencyValue(
                    rate.currency,
                    rate.value * amount
                )
            )
            secondaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
        } else {
            primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)
            secondaryAmountInfo = null
        }

        val date = if (transaction.timestamp == 0L) null else Date(transaction.timestamp * 1000)

        val titleViewItem = TransactionInfoModule.TitleViewItem(
            date,
            primaryAmountInfo,
            secondaryAmountInfo,
            transactionType,
            lockState
        )

        titleLiveData.postValue(titleViewItem)

        val viewItems = mutableListOf<TransactionDetailViewItem>()

        viewItems.add(
            TransactionDetailViewItem.Status(
                status,
                transactionType is TransactionType.Incoming
            )
        )

        rate?.let {
            viewItems.add(TransactionDetailViewItem.Rate(rate, wallet.coin.code))
        }

//        transaction.fee?.let { fee ->
//            val feeCoin = interactor.feeCoin(coin) ?: coin
//            viewItems.add(TransactionDetailViewItem.Fee(CoinValue(feeCoin, fee), rate?.let { CurrencyValue(it.currency, it.value * fee) }))
//        }

//        transaction.from?.let { from ->
//            if (showFromAddress(wallet.coin.type)) {
//                viewItems.add(TransactionDetailViewItem.From(transactionInfoAddressMapper.map(from)))
//            }
//        }
//
//        transaction.to?.let { to ->
//            viewItems.add(TransactionDetailViewItem.To(transactionInfoAddressMapper.map(to)))
//        }
//
//        transaction.memo?.let { memo ->
//            viewItems.add(TransactionDetailViewItem.Memo(memo))
//        }
//
//        transaction.lockInfo?.originalAddress?.let { recipient ->
//            if (transaction.type == TransactionType.Outgoing) {
//                viewItems.add(TransactionDetailViewItem.Recipient(transactionInfoAddressMapper.map(recipient)))
//            }
//        }
//
//        if (transaction.showRawTransaction) {
//            viewItems.add(TransactionDetailViewItem.RawTransaction())
//        } else {
        viewItems.add(TransactionDetailViewItem.Id(transaction.transactionHash))
//        }
//
//        if (transaction.conflictingTxHash != null) {
//            viewItems.add(TransactionDetailViewItem.DoubleSpend())
//        }

        lockState?.let {
            viewItems.add(TransactionDetailViewItem.LockInfo(it))
        }

//        if (transaction.type == TransactionType.SentToSelf) {
//            viewItems.add(TransactionDetailViewItem.SentToSelf())
//        }

        detailsLiveData.postValue(viewItems)

        explorerButton.postValue(Pair(explorerData.title, explorerData.url != null))

    }

    fun onShare() {
        showShareLiveEvent.value = transaction.transactionHash
    }

    fun openExplorer() {
        explorerData.url?.let {
            showTransactionLiveEvent.postValue(it)
        }
    }

    fun onClickLockInfo() {
//        transaction.lockInfo?.lockedUntil?.let {
//        showLockInfo.postValue(it)
//        }
    }

    fun onClickDoubleSpendInfo() {
//        transaction.conflictingTxHash?.let { conflictingTxHash ->
//        showDoubleSpendInfo.postValue(Pair(transaction.transactionHash, conflictingTxHash))
//        }
    }

    fun onClickRecipientHash() {
//        onCopy(transaction.lockInfo?.originalAddress)
    }

    fun onClickTo() {
//        onCopy(transaction.to)
    }

    fun onClickFrom() {
//        onCopy(transaction.from)
    }

    fun onClickTransactionId() {
        onCopy(transaction.transactionHash)
    }

    fun onRawTransaction() {
        onCopy(service.getRaw(transaction.transactionHash))
    }

    fun onClickStatusInfo() {
        showStatusInfoLiveEvent.call()
    }

    private fun onCopy(value: String?) {
        value?.let {
            service.copyToClipboard(value)
            showCopiedLiveEvent.call()
        }
    }

    private fun showFromAddress(coinType: CoinType): Boolean {
        return !(coinType == CoinType.Bitcoin || coinType == CoinType.BitcoinCash || coinType == CoinType.Dash)
    }

    private fun getExplorerData(
        hash: String,
        testMode: Boolean,
        coinType: CoinType
    ): TransactionInfoModule.ExplorerData {
        return when (coinType) {
            is CoinType.Bitcoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/bitcoin/transaction/$hash"
            )
            is CoinType.BitcoinCash -> TransactionInfoModule.ExplorerData(
                "btc.com",
                if (testMode) null else "https://bch.btc.com/$hash"
            )
            is CoinType.Litecoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/litecoin/transaction/$hash"
            )
            is CoinType.Dash -> TransactionInfoModule.ExplorerData(
                "dash.org",
                if (testMode) null else "https://insight.dash.org/insight/tx/$hash"
            )
            is CoinType.Ethereum,
            is CoinType.Erc20 -> {
                val domain = when (service.ethereumNetworkType(wallet.account)) {
                    EthereumKit.NetworkType.EthMainNet -> "etherscan.io"
                    EthereumKit.NetworkType.EthRopsten -> "ropsten.etherscan.io"
                    EthereumKit.NetworkType.EthKovan -> "kovan.etherscan.io"
                    EthereumKit.NetworkType.EthRinkeby -> "rinkeby.etherscan.io"
                    EthereumKit.NetworkType.EthGoerli -> "goerli.etherscan.io"
                    EthereumKit.NetworkType.BscMainNet -> throw IllegalArgumentException("")
                }
                TransactionInfoModule.ExplorerData("etherscan.io", "https://$domain/tx/$hash")
            }
            is CoinType.Bep2 -> TransactionInfoModule.ExplorerData(
                "binance.org",
                if (testMode) "https://testnet-explorer.binance.org/tx/$hash" else "https://explorer.binance.org/tx/$hash"
            )
            is CoinType.BinanceSmartChain,
            is CoinType.Bep20 -> TransactionInfoModule.ExplorerData(
                "bscscan.com",
                "https://bscscan.com/tx/$hash"
            )
            is CoinType.Zcash -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/zcash/transaction/$hash"
            )
            is CoinType.Unsupported -> throw IllegalArgumentException()
        }
    }

}
