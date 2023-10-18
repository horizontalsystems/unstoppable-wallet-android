package cash.p.terminal.core.managers

import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import java.math.BigDecimal

class SpamManager(
    private val marketKitWrapper: MarketKitWrapper
) {
    private val negligibleValue = BigDecimal("0.01")
    private var cachedUsdPrices = mutableMapOf<String, BigDecimal?>()

    fun isSpam(
        incomingEvents: List<TransferEvent>,
        outgoingEvents: List<TransferEvent>
    ): Boolean {
        val allEvents = incomingEvents + outgoingEvents
        return allEvents.all { spamEvent(it) }
    }

    private fun spamEvent(event: TransferEvent): Boolean {
        return when (val eventValue = event.value) {
            is TransactionValue.CoinValue -> {
                spamValue(eventValue.coinUid, eventValue.value)
            }

            is TransactionValue.NftValue -> {
                spamValue(eventValue.coinUid, eventValue.value)
            }

            else -> true
        }
    }

    private fun spamValue(coinUid: String, value: BigDecimal): Boolean {
        return cachedUsdPrice(coinUid)?.let { usdPrice ->
            usdPrice * value < negligibleValue
        } ?: run {
            value <= BigDecimal.ZERO
        }
    }

    private fun cachedUsdPrice(coinUid: String): BigDecimal? {
        return cachedUsdPrices.getOrPut(coinUid) {
            marketKitWrapper.coinPrice(coinUid, "USD")?.value
        }
    }
}
