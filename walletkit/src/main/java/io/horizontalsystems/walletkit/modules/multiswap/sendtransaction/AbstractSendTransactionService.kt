package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ServiceState
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.util.UUID

abstract class AbstractSendTransactionService(val hasSettings: Boolean, val hasNonceSettings: Boolean): ServiceState<SendTransactionServiceState>() {
    open val supportsMevProtection: Boolean = false
    /** Explanation shown for the network fee row, or null for the generic one. */
    open val networkFeeInfoRes: Int? = null

    /**
     * The most of the token this service can send under its current fee estimate, for chains
     * where "requested amount minus fee" is not the answer (input selection, rent reserves).
     * Null means that plain rule applies. Read after the state has settled.
     */
    open fun maxSendableAmount(): BigDecimal? = null
    abstract val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings>
    protected var uuid = UUID.randomUUID().toString()

    private val baseCurrency = App.currencyManager.baseCurrency

    abstract fun start(coroutineScope: CoroutineScope)

    /**
     * Releases what [start] set up outside the scope it was given (sub-services with their
     * own scopes). The owning view model calls this when cleared.
     */
    open fun clear() = Unit

    abstract suspend fun setSendTransactionData(data: SendTransactionData)

    /**
     * A new, unstarted service for the same token that starts from this one's fee choices, for a
     * deposit whose destination is only known right before signing (a swap provider's deposit
     * address arrives with the order, after the confirmation screen estimated against a stand-in).
     * Null when this service reflects a replaced [setSendTransactionData] as soon as the call
     * returns, so it can send the new deposit itself. Services that spread the data over
     * sub-services collected asynchronously must return a fresh instance: their state keeps
     * describing the previous destination until those collectors run, and sending in that window
     * would pay the stand-in.
     */
    open fun fresh(): AbstractSendTransactionService? = null

    /**
     * Suspends until the state describes the current transaction data: not loading, and either
     * sendable or carrying an error caution. A state that never settles within [timeoutMs] is a
     * failed estimate and throws [IllegalStateException], never a cancellation: callers treat
     * cancellation as the user leaving and skip the recovery a failed deposit needs.
     */
    suspend fun awaitSettled(timeoutMs: Long = 60_000): SendTransactionServiceState =
        withTimeoutOrNull(timeoutMs) {
            stateFlow.first { state ->
                !state.loading && (state.sendable || state.cautions.any { it.type == CautionViewItem.Type.Error })
            }
        } ?: throw IllegalStateException("The transaction could not be estimated in time")
    @Composable
    open fun GetSettingsContent(navigation: HSNavigation) = Unit
    @Composable
    open fun GetNonceSettingsContent(navigation: HSNavigation) = Unit
    abstract suspend fun sendTransaction(mevProtectionEnabled: Boolean = false): SendTransactionResult

    /**
     * First call in every [sendTransaction] override. UI tests in E2E builds use mainnet wallets
     * and stop at the confirmation screen; this makes a flow that taps the final button anyway
     * fail before anything is signed. Overrides keep their narrower result types, which is why
     * this is a call in each one rather than a wrapper around them.
     */
    protected fun checkBroadcastAllowed() {
        if (App.appConfigProvider.e2eBuild) throw BroadcastDisabledException()
    }

    fun refreshUuid() {
        uuid = UUID.randomUUID().toString()
    }

    protected fun getAmountData(coinValue: CoinValue): SendModule.AmountData {
        val primaryAmountInfo = SendModule.AmountInfo.CoinValueInfo(coinValue)

        val secondaryAmountInfo = getRate(coinValue.coin)?.let { rate ->
            SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(rate.currency, rate.value * coinValue.value))
        }
        return SendModule.AmountData(primaryAmountInfo, secondaryAmountInfo)
    }

    private fun getRate(coin: Coin): CurrencyValue? {
        return App.marketKit.coinPrice(coin.uid, baseCurrency.code)?.let {
            CurrencyValue(baseCurrency, it.value)
        }
    }
}

data class SendTransactionServiceState(
    val uuid: String,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val sendable: Boolean,
    val loading: Boolean,
    val fields: List<DataField>,
)

class BroadcastDisabledException : IllegalStateException("Broadcasting is disabled in E2E builds")

/** Kit-free projection of a signed raw transaction for flows that submit externally. */
data class SignedTransactionInfo(val hex: String, val transactionHash: String)

/** Send services that can sign without broadcasting (e.g. OpenCryptoPay proof-of-payment). */
interface ISignOnlySendTransactionService {
    fun signTransaction(): SignedTransactionInfo
}
