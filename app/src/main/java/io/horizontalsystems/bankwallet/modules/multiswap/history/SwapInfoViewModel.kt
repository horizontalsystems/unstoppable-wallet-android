package io.horizontalsystems.bankwallet.modules.multiswap.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.alternativeImageUrl
import io.horizontalsystems.bankwallet.core.coinIconUrl
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.multiswap.providers.MultiSwapProviderRegistry
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

class SwapInfoViewModel(
    private val recordId: Int,
    private val swapRecordManager: SwapRecordManager,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModelUiState<SwapInfoUiState>() {

    private var tokenInImageUrl: String = ""
    private var tokenInAlternativeImageUrl: String? = null
    private var tokenInCode: String = ""
    private var tokenInBadge: String? = null
    private var tokenOutImageUrl: String = ""
    private var tokenOutAlternativeImageUrl: String? = null
    private var tokenOutCode: String = ""
    private var tokenOutBadge: String? = null
    private var amountIn: String = ""
    private var amountOut: String? = null
    private var fiatAmountIn: String? = null
    private var fiatAmountOut: String? = null
    private var providerName: String = ""
    private var formattedDate: String = ""
    private var status: SwapStatus = SwapStatus.Depositing
    private var recipientAddress: String? = null
    private var depositingTxUrl: String? = null
    private var sendingTxUrl: String? = null
    private var isSingleChain: Boolean = false

    override fun createState() = SwapInfoUiState(
        tokenInImageUrl = tokenInImageUrl,
        tokenInAlternativeImageUrl = tokenInAlternativeImageUrl,
        tokenInCode = tokenInCode,
        tokenInBadge = tokenInBadge,
        tokenOutImageUrl = tokenOutImageUrl,
        tokenOutAlternativeImageUrl = tokenOutAlternativeImageUrl,
        tokenOutCode = tokenOutCode,
        tokenOutBadge = tokenOutBadge,
        amountIn = amountIn,
        amountOut = amountOut,
        fiatAmountIn = fiatAmountIn,
        fiatAmountOut = fiatAmountOut,
        providerName = providerName,
        formattedDate = formattedDate,
        status = status,
        recipientAddress = recipientAddress,
        depositingTxUrl = depositingTxUrl,
        sendingTxUrl = sendingTxUrl,
        isSingleChain = isSingleChain,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) { loadData() }
        viewModelScope.launch(Dispatchers.IO) {
            swapRecordManager.recordsUpdatedFlow.collect { loadData() }
        }
    }

    private suspend fun loadData() {
        val record = swapRecordManager.getById(recordId) ?: return
        val currency = currencyManager.baseCurrency
        val timestampSeconds = record.timestamp / 1000
        val priceIn = fetchHistoricalPrice(record.tokenInCoinUid, currency.code, timestampSeconds)
        val priceOut = fetchHistoricalPrice(record.tokenOutCoinUid, currency.code, timestampSeconds)

        val coins = marketKit.fullCoins(listOf(record.tokenInCoinUid, record.tokenOutCoinUid))
            .associateBy { it.coin.uid }
        tokenInImageUrl = record.tokenInCoinUid.coinIconUrl
        tokenInAlternativeImageUrl = coins[record.tokenInCoinUid]?.coin?.alternativeImageUrl
        tokenInCode = record.tokenInCoinCode
        tokenInBadge = record.tokenInBadge
        tokenOutImageUrl = record.tokenOutCoinUid.coinIconUrl
        tokenOutAlternativeImageUrl = coins[record.tokenOutCoinUid]?.coin?.alternativeImageUrl
        tokenOutCode = record.tokenOutCoinCode
        tokenOutBadge = record.tokenOutBadge
        amountIn = formatAmount(record.amountIn, record.tokenInCoinCode)
        amountOut = record.amountOut?.let { formatAmount(it, record.tokenOutCoinCode) }
        fiatAmountIn = formatFiat(record.amountIn, priceIn, currency.symbol, currency.decimal)
        fiatAmountOut = record.amountOut?.let { formatFiat(it, priceOut, currency.symbol, currency.decimal) }
        providerName = record.providerName
        formattedDate = DateHelper.formatDate(Date(record.timestamp), "MMM d, yyyy, HH:mm")
        status = runCatching { SwapStatus.valueOf(record.status) }.getOrDefault(SwapStatus.Depositing)
        recipientAddress = record.recipientAddress
        depositingTxUrl = record.transactionHash?.let { buildTxUrl(record.tokenInBlockchainTypeUid, it) }
        sendingTxUrl = record.outboundTransactionHash?.let { buildTxUrl(record.tokenOutBlockchainTypeUid, it) }
        isSingleChain = MultiSwapProviderRegistry.isSingleChainSwap(
            record.providerId,
            record.tokenInBlockchainTypeUid,
            record.tokenOutBlockchainTypeUid,
        )

        emitState()
    }

    private fun formatAmount(amountStr: String, coinCode: String): String {
        val amount = amountStr.toBigDecimalOrNull() ?: return amountStr
        return numberFormatter.formatCoinShort(amount, coinCode, 8)
    }

    private suspend fun fetchHistoricalPrice(coinUid: String, currencyCode: String, timestampSeconds: Long): BigDecimal? {
        return try {
            marketKit.coinHistoricalPrice(coinUid, currencyCode, timestampSeconds)?.let { return it }
            val rate = marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestampSeconds).await()
            if (rate.compareTo(BigDecimal.ZERO) != 0) rate else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatFiat(amountStr: String, price: BigDecimal?, symbol: String, decimals: Int): String? {
        val amount = amountStr.toBigDecimalOrNull() ?: return null
        val price = price ?: return null
        val fiat = (amount * price).setScale(decimals, RoundingMode.DOWN).stripTrailingZeros()
        return numberFormatter.formatFiatShort(fiat, symbol, decimals)
    }

    private fun buildTxUrl(blockchainTypeUid: String, txHash: String): String? =
        when (BlockchainType.fromUid(blockchainTypeUid)) {
            BlockchainType.Bitcoin -> "https://blockchair.com/bitcoin/transaction/$txHash"
            BlockchainType.BitcoinCash -> "https://blockchair.com/bitcoin-cash/transaction/$txHash"
            BlockchainType.ECash -> "https://blockchair.com/ecash/transaction/$txHash"
            BlockchainType.Litecoin -> "https://blockchair.com/litecoin/transaction/$txHash"
            BlockchainType.Dash -> "https://blockchair.com/dash/transaction/$txHash"
            BlockchainType.Zcash -> "https://blockchair.com/zcash/transaction/$txHash"
            BlockchainType.Monero -> "https://blockchair.com/monero/transaction/$txHash"
            BlockchainType.Ethereum -> "https://etherscan.io/tx/$txHash"
            BlockchainType.BinanceSmartChain -> "https://bscscan.com/tx/$txHash"
            BlockchainType.Polygon -> "https://polygonscan.com/tx/$txHash"
            BlockchainType.Optimism -> "https://optimistic.etherscan.io/tx/$txHash"
            BlockchainType.ArbitrumOne -> "https://arbiscan.io/tx/$txHash"
            BlockchainType.Avalanche -> "https://snowtrace.io/tx/$txHash"
            BlockchainType.Gnosis -> "https://gnosisscan.io/tx/$txHash"
            BlockchainType.Fantom -> "https://ftmscan.com/tx/$txHash"
            BlockchainType.Base -> "https://basescan.org/tx/$txHash"
            BlockchainType.ZkSync -> "https://era.zksync.network/tx/$txHash"
            BlockchainType.Solana -> "https://solscan.io/tx/$txHash"
            BlockchainType.Tron -> "https://tronscan.io/#/transaction/$txHash"
            BlockchainType.Ton -> "https://tonviewer.com/transaction/$txHash"
            BlockchainType.Stellar -> "https://stellar.expert/explorer/public/tx/$txHash"
            is BlockchainType.Unsupported -> null
        }

    class Factory(private val recordId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapInfoViewModel(
                recordId = recordId,
                swapRecordManager = App.swapRecordManager,
                marketKit = App.marketKit,
                currencyManager = App.currencyManager,
                numberFormatter = App.numberFormatter,
            ) as T
        }
    }
}

data class SwapInfoUiState(
    val tokenInImageUrl: String,
    val tokenInAlternativeImageUrl: String?,
    val tokenInCode: String,
    val tokenInBadge: String?,
    val tokenOutImageUrl: String,
    val tokenOutAlternativeImageUrl: String?,
    val tokenOutCode: String,
    val tokenOutBadge: String?,
    val amountIn: String,
    val amountOut: String?,
    val fiatAmountIn: String?,
    val fiatAmountOut: String?,
    val providerName: String,
    val formattedDate: String,
    val status: SwapStatus,
    val recipientAddress: String?,
    val depositingTxUrl: String?,
    val sendingTxUrl: String?,
    val isSingleChain: Boolean,
)
