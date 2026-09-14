package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IActivatableTokenAdapter
import io.horizontalsystems.walletkit.core.TokenActivationError
import io.horizontalsystems.stellarkit.EnablingAssetError
import io.horizontalsystems.walletkit.core.managers.StellarKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StellarAssetAdapter(
    stellarKitWrapper: StellarKitWrapper,
    code: String,
    issuer: String
) : BaseStellarAdapter(stellarKitWrapper), IActivatableTokenAdapter {

    private val stellarAsset = StellarAsset.Asset(code, issuer)
    private var assetBalance: BigDecimal? = null

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val balance: BigDecimal
        get() = assetBalance ?: BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = _balanceStateUpdatedFlow
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlow: Flow<Unit>
        get() = _balanceUpdatedFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val activationFee: BigDecimal get() = stellarKit.sendFee

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(stellarAsset).collect { balance ->
                assetBalance = balance?.balance
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }

    override val fee: BigDecimal
        get() = stellarKit.sendFee

    override val maxSendableBalance: BigDecimal
        get() = balance

    override suspend fun getMinimumSendAmount(address: String) = null

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        stellarKit.sendAsset(stellarAsset.id, address, amount, memo)
    }

    override fun validate(address: String) {
        StellarKit.validateAddress(address)

        if (!stellarKit.isAssetEnabled(stellarAsset, address)) {
            throw NoTrustlineError(stellarAsset.code)
        }
    }

    suspend fun isTrustlineEstablished() = withContext(Dispatchers.Default) {
        assetBalance != null || stellarKit.isAssetEnabled(stellarAsset)
    }

    // IActivatableTokenAdapter

    override suspend fun isActivated(): Boolean = isTrustlineEstablished()

    override suspend fun activate() = withContext(Dispatchers.IO) {
        stellarKit.enableAsset(stellarAsset.id, null)
    }

    override fun validateActivation() {
        try {
            stellarKit.validateEnablingAsset()
        } catch (e: EnablingAssetError.InsufficientBalance) {
            throw TokenActivationError.InsufficientBalance()
        }
    }

}
