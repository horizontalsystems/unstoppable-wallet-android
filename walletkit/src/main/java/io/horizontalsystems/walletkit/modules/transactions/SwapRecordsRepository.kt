package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.core.Clearable
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapOperation
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.multiswap.providers.MultiSwapProviderRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

data class SwapRecordItem(
    val record: SwapRecord,
    val kind: SwapRecordKind,
    val status: SwapStatus,
    val tokenInAlternativeImageUrl: String?,
    val tokenOutAlternativeImageUrl: String?,
    // Only a private send shows a fiat value: its amount is one coin, not a pair.
    val fiatIn: CurrencyValue?,
)

/** The active account's swap records, as the activity list shows them. */
class SwapRecordsRepository(
    private val swapRecordManager: SwapRecordManager,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) : Clearable {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val confidentialProviderIds = MultiSwapProviderRegistry.confidentialProviders.map { it.id }.toSet()

    // Null until the records are first read.
    private val _itemsFlow = MutableStateFlow<List<SwapRecordItem>?>(null)
    val itemsFlow: StateFlow<List<SwapRecordItem>?> = _itemsFlow.asStateFlow()

    // Keyed by record id; dropped with the currency it was fetched in.
    private val fiatRates = ConcurrentHashMap<Int, CurrencyValue>()
    // Record ids whose rate was requested in the current currency, found or not: records are
    // reloaded on every status update, and a coin without a price would be asked for again each time.
    private val rateRequested = ConcurrentHashMap.newKeySet<Int>()
    @Volatile
    private var ratesCurrency = currencyManager.baseCurrency

    fun start() {
        coroutineScope.launch {
            merge(swapRecordManager.recordsUpdatedFlow, currencyManager.baseCurrencyUpdatedFlow).collect {
                val currency = currencyManager.baseCurrency
                if (currency != ratesCurrency) {
                    fiatRates.clear()
                    rateRequested.clear()
                    ratesCurrency = currency
                }
                load()
            }
        }
    }

    // The records belong to the active account, which changes without a record update.
    fun reload() {
        coroutineScope.launch { load() }
    }

    @Synchronized
    private fun load() {
        val records = swapRecordManager.getAll()
        val coinUids = records.flatMap { listOf(it.tokenInCoinUid, it.tokenOutCoinUid) }.distinct()
        val alternativeUrls = try {
            marketKit.fullCoins(coinUids).associate { it.coin.uid to it.coin.alternativeImageUrl }
        } catch (_: Throwable) {
            emptyMap()
        }

        val items = records.map { record ->
            val kind = SwapRecordKind.of(SwapOperation.fromString(record.operation), record.providerId, confidentialProviderIds)
            SwapRecordItem(
                record = record,
                kind = kind,
                status = runCatching { SwapStatus.valueOf(record.status) }.getOrDefault(SwapStatus.Depositing),
                tokenInAlternativeImageUrl = alternativeUrls[record.tokenInCoinUid],
                tokenOutAlternativeImageUrl = alternativeUrls[record.tokenOutCoinUid],
                fiatIn = fiatRates[record.id]?.let { rate -> fiatValue(record.amountIn, rate) },
            )
        }

        _itemsFlow.value = items
        items
            .filter { it.kind == SwapRecordKind.PrivateSend && it.fiatIn == null }
            .filter { rateRequested.add(it.record.id) }
            .forEach { fetchRate(it.record) }
    }

    private fun fetchRate(record: SwapRecord) {
        coroutineScope.launch {
            val currency = currencyManager.baseCurrency
            val timestamp = record.timestamp / 1000
            val price = try {
                marketKit.coinHistoricalPrice(record.tokenInCoinUid, currency.code, timestamp)
                    ?: marketKit.coinHistoricalPriceSingle(record.tokenInCoinUid, currency.code, timestamp)
            } catch (_: Throwable) {
                null
            }
            if (price == null || price.compareTo(BigDecimal.ZERO) == 0) return@launch
            // The currency changed while the price was fetched; the reload asks again in the new one.
            if (currency != ratesCurrency) return@launch

            val rate = CurrencyValue(currency, price)
            fiatRates[record.id] = rate
            _itemsFlow.update { items ->
                items?.map { item ->
                    if (item.record.id == record.id) item.copy(fiatIn = fiatValue(record.amountIn, rate)) else item
                }
            }
        }
    }

    private fun fiatValue(amount: String, rate: CurrencyValue): CurrencyValue? {
        val decimal = amount.toBigDecimalOrNull() ?: return null
        return CurrencyValue(rate.currency, decimal * rate.value)
    }

    override fun clear() {
        coroutineScope.cancel()
    }
}
