package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import java.math.BigDecimal

class SpamManager(
    private val localStorage: ILocalStorage
) {
    private val stableCoinCodes = listOf("USDT", "USDC", "DAI", "BUSD", "EURS")
    private val negligibleValue = BigDecimal("0.01")

    var hideUnknownTokens = localStorage.hideUnknownTokens
        private set
    var hideStablecoinsDust = localStorage.hideStablecoinsNegligibleAmount
        private set

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
                spamValue(eventValue.coinCode, eventValue.value)
            }

            is TransactionValue.NftValue -> {
                eventValue.value <= BigDecimal.ZERO
            }

            else -> hideUnknownTokens
        }
    }

    private fun spamValue(coinCode: String, value: BigDecimal): Boolean {
        return if (hideStablecoinsDust && stableCoinCodes.contains(coinCode)) {
            value < negligibleValue
        } else {
            value <= BigDecimal.ZERO
        }
    }

    fun updateFilterHideUnknownTokens(hide: Boolean) {
        localStorage.hideUnknownTokens = hide
        hideUnknownTokens = hide
    }

    fun updateFilterHideStablecoinsDust(hide: Boolean) {
        localStorage.hideStablecoinsNegligibleAmount = hide
        hideStablecoinsDust = hide
    }

}
