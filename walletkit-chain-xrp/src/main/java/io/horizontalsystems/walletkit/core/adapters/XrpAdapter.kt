package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.ReserveInfo
import io.horizontalsystems.walletkit.core.ReserveItem
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.managers.XrpKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.core.providers.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

/** Balance, receive and send adapter for native XRP. */
class XrpAdapter(
    xrpKitWrapper: XrpKitWrapper,
) : BaseXrpAdapter(xrpKitWrapper) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override var balanceState: AdapterState = AdapterState.Syncing()

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val balanceUpdatedFlow: Flow<Unit> get() = _balanceUpdatedFlow
    override val balanceStateUpdatedFlow: Flow<Unit> get() = _balanceStateUpdatedFlow

    override val balanceData: BalanceData
        get() = BalanceData(
            available = kit.availableBalance.xrp,
            minimumBalance = kit.minimumBalance.xrp,
            reserve = reserveInfo(),
        )

    /** Last fee the network reported; refreshed on start so the send form has a value immediately. */
    override var fee: BigDecimal = DEFAULT_FEE
        private set

    override fun start() {
        coroutineScope.launch {
            kit.accountStateFlow.collectSafely { _balanceUpdatedFlow.tryEmit(Unit) }
        }
        coroutineScope.launch {
            kit.ledgerStateFlow.collectSafely { _balanceUpdatedFlow.tryEmit(Unit) }
        }
        coroutineScope.launch {
            kit.trustLinesFlow.collectSafely { _balanceUpdatedFlow.tryEmit(Unit) }
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
                // keep the default; the kit re-reads the fee at send time anyway
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    private fun reserveInfo(): ReserveInfo? {
        if (!kit.isAccountActivated) return null

        val baseReserve = kit.baseReserve.xrp.stripTrailingZeros().toPlainString()
        val ownerReserve = kit.ownerReserve.xrp.stripTrailingZeros().toPlainString()
        val items = mutableListOf(
            ReserveItem(Translator.getString(R.string.Info_Reserved_WalletAction), "$baseReserve XRP")
        )
        val trustLines = kit.trustLines
        trustLines.forEach { line ->
            items.add(ReserveItem(XrpKit.displayCurrencyCode(line.currency), "$ownerReserve XRP"))
        }
        val otherObjects = kit.accountState.ownerCount - trustLines.size
        if (otherObjects > 0) {
            items.add(
                ReserveItem(
                    Translator.getString(R.string.Info_Reserved_Xrp_OtherObjects, otherObjects),
                    "${kit.ownerReserve.xrp.multiply(BigDecimal(otherObjects)).stripTrailingZeros().toPlainString()} XRP"
                )
            )
        }
        return ReserveInfo(items, Translator.getString(R.string.Info_Reserved_Xrp_Description))
    }

    // ISendXrpAdapter

    override val maxSendableBalance: BigDecimal
        get() = (kit.availableBalance.xrp - fee).max(BigDecimal.ZERO)

    override suspend fun getMinimumSendAmount(address: String): BigDecimal? {
        return if (kit.doesAccountExist(classicAddress(address))) null else kit.baseReserve.xrp
    }

    override suspend fun send(amount: BigDecimal, address: String, destinationTag: Long?, memo: String?): String {
        return kit.sendXrp(address, amount, destinationTag, memo).hash
    }
}
