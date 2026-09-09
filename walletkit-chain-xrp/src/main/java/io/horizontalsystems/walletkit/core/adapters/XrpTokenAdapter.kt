package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IActivatableTokenAdapter
import io.horizontalsystems.walletkit.core.TokenActivationError
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.XrpKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

/**
 * Adapter for an XRPL issued currency held over a trust line. The line must exist before the
 * token can be received; creating it costs one owner-reserve increment while it exists.
 */
class XrpTokenAdapter(
    xrpKitWrapper: XrpKitWrapper,
    val currency: String,
    val issuer: String,
) : BaseXrpAdapter(xrpKitWrapper), IActivatableTokenAdapter {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private var tokenBalance: BigDecimal? = null
    private var frozen = false

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlow: Flow<Unit> get() = _balanceStateUpdatedFlow
    override val balanceData: BalanceData get() = BalanceData(tokenBalance ?: BigDecimal.ZERO)
    override val balanceUpdatedFlow: Flow<Unit> get() = _balanceUpdatedFlow

    /** Network fee of the TrustSet transaction. The 0.2 XRP reserve is locked, not spent. */
    override val activationFee: BigDecimal get() = fee
    val activationReserve: BigDecimal get() = kit.ownerReserve.xrp

    override var fee: BigDecimal = DEFAULT_FEE
        private set

    override fun start() {
        coroutineScope.launch {
            kit.trustLinesFlow.collectSafely { lines ->
                val line = lines.firstOrNull { it.currency == currency && it.issuer == issuer }
                tokenBalance = line?.balance
                frozen = line?.frozen ?: false
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            kit.syncStateFlow.collectSafely {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            try {
                fee = kit.estimateFee().xrp
            } catch (e: Exception) {
                // keep the default
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    // ISendXrpAdapter

    override val maxSendableBalance: BigDecimal
        get() = if (frozen) BigDecimal.ZERO else tokenBalance ?: BigDecimal.ZERO

    override suspend fun getMinimumSendAmount(address: String): BigDecimal? = null

    override suspend fun send(amount: BigDecimal, address: String, destinationTag: Long?, memo: String?): String {
        return kit.sendToken(currency, issuer, address, amount, destinationTag, memo).hash
    }

    override fun validate(address: String) {
        XrpKit.validateAddress(address)
    }

    // trust line activation

    // IActivatableTokenAdapter

    override suspend fun isActivated(): Boolean = withContext(Dispatchers.Default) {
        tokenBalance != null || kit.isTrustLineSet(currency, issuer)
    }

    /** The spendable XRP must cover the trust line's owner reserve plus the TrustSet fee. */
    override fun validateActivation() {
        if (!kit.isAccountActivated || kit.availableBalance.xrp < kit.ownerReserve.xrp + fee) {
            throw TokenActivationError.InsufficientBalance()
        }
    }

    override suspend fun activate() {
        kit.setTrustLine(currency, issuer)
    }
}
