package cash.p.terminal.core.adapters.stellar

import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.managers.StellarKitWrapper
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.room.StellarAsset
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StellarAssetAdapter(
    stellarKitWrapper: StellarKitWrapper,
    code: String,
    issuer: String
) : BaseStellarAdapter(stellarKitWrapper), ISendStellarAdapter {

    private val stellarAsset = StellarAsset.Asset(code, issuer)
    private var assetBalance: BigDecimal? = null

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val balance: BigDecimal
        get() = assetBalance ?: BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = balanceStateUpdatedSubject.asFlow()
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlow: Flow<Unit>
        get() = balanceUpdatedSubject.asFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    val activationFee = stellarKit.sendFee

    override fun start() {
        coroutineScope.launch {
            stellarKit.getBalanceFlow(stellarAsset).collect { balance ->
                assetBalance = balance?.balance
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override suspend fun refresh() = Unit
    override val statusInfo: Map<String, Any>
        get() = TODO("Not yet implemented")

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

    fun activate() {
        stellarKit.enableAsset(stellarAsset.id, null)
    }

    fun validateActivation() {
        stellarKit.validateEnablingAsset()
    }

    data class NoTrustlineError(val code: String) : Error()
}
