package cash.p.terminal.core.adapters.stellar

import cash.p.terminal.core.INativeBalanceProvider
import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.managers.StellarKitWrapper
import cash.p.terminal.core.managers.statusInfo
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class StellarAssetAdapter(
    stellarKitWrapper: StellarKitWrapper,
    code: String,
    issuer: String
) : BaseStellarAdapter(stellarKitWrapper), ISendStellarAdapter, INativeBalanceProvider {

    private val stellarAsset = StellarAsset.Asset(code, issuer)
    private var assetBalance: BigDecimal? = null
    private var xlmBalance: BigDecimal = BigDecimal.ZERO

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val nativeBalanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val balance: BigDecimal
        get() = assetBalance ?: BigDecimal.ZERO

    private val _balanceState = MutableStateFlow(stellarKit.syncStateFlow.value.toAdapterState())
    override val balanceState: AdapterState get() = _balanceState.value
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = _balanceState.map { }
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
            stellarKit.getBalanceFlow(StellarAsset.Native).collect { balance ->
                xlmBalance = balance?.balance ?: BigDecimal.ZERO
                nativeBalanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            stellarKit.syncStateFlow.collect {
                _balanceState.value = it.toAdapterState()
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override suspend fun refresh() = Unit
    override val statusInfo: Map<String, Any>
        get() = stellarKit.statusInfo()

    // INativeBalanceProvider

    override val nativeBalanceData: BalanceData
        get() = BalanceData(xlmBalance)

    override val nativeBalanceUpdatedFlow: Flow<Unit>
        get() = nativeBalanceUpdatedSubject.asFlow()

    // Fee is ZERO because Stellar asset transfers are paid in XLM (native token), not the asset itself.
    // Returning the actual XLM fee here would cause incorrect subtraction when swapping 100% of asset balance.
    override val fee: StateFlow<BigDecimal> = MutableStateFlow(BigDecimal.ZERO)

    override val maxSpendableBalance: BigDecimal
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

    suspend fun activate() {
        stellarKit.enableAsset(stellarAsset.id, null)
    }

    fun validateActivation() {
        stellarKit.validateEnablingAsset()
    }

    data class NoTrustlineError(val code: String) : Error()
}
