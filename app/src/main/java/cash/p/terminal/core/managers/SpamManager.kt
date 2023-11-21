package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import java.math.BigDecimal

class SpamManager(
    private val localStorage: ILocalStorage
) {
    private val stableCoinCodes = listOf("USDT", "USDC", "DAI", "BUSD", "EURS")
    private val negligibleValue = BigDecimal("0.01")

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
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
                spamValue(eventValue.coinUid, eventValue.value)
            }

            else -> hideSuspiciousTx
        }
    }

    private fun spamValue(coinCode: String, value: BigDecimal): Boolean {
        return if (hideSuspiciousTx && stableCoinCodes.contains(coinCode)) {
            value < negligibleValue
        } else {
            value <= BigDecimal.ZERO
        }
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

}
