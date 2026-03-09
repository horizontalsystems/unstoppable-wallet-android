package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.INativeBalanceProvider
import cash.p.terminal.core.ISendTonAdapter
import cash.p.terminal.core.managers.TonKitWrapper
import cash.p.terminal.core.managers.toAdapterState
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.FriendlyAddress
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.math.BigInteger

class JettonAdapter(
    coinManager: ICoinManager,
    tonKitWrapper: TonKitWrapper,
    addressStr: String,
    wallet: Wallet,
) : BaseTonAdapter(tonKitWrapper, wallet.decimal), ISendTonAdapter, INativeBalanceProvider {

    private val address = Address.parse(addressStr)
    private var jettonBalance = tonKit.jettonBalanceMap[address]

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val balance: BigDecimal
        get() = jettonBalance?.balance?.toBigDecimal()?.movePointLeft(decimals)
            ?: BigDecimal.ZERO

    private val _balanceState = MutableStateFlow(tonKit.jettonSyncStateFlow.value.toAdapterState())
    override val balanceState: AdapterState get() = _balanceState.value
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = _balanceState.map { }
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlow: Flow<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER).asFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val nativeToken: Token =
        requireNotNull(coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native)))

    override fun start() {
        coroutineScope.launch {
            tonKit.jettonBalanceMapFlow.collect { jettonBalanceMap ->
                jettonBalance = jettonBalanceMap[address]
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.jettonSyncStateFlow.collect {
                _balanceState.value = it.toAdapterState()
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override suspend fun refresh() {
    }

    override val fee: StateFlow<BigDecimal> = MutableStateFlow(BigDecimal.ZERO)

    // INativeBalanceProvider

    override val nativeBalanceData: BalanceData
        get() = BalanceData(tonKit.account?.balance?.toBigDecimal()?.movePointLeft(nativeToken.decimals) ?: BigDecimal.ZERO)

    override val nativeBalanceUpdatedFlow: Flow<Unit>
        get() = tonKit.accountFlow.map { }

    override suspend fun send(amount: BigDecimal, address: FriendlyAddress, memo: String?) {
        tonKit.send(
            jettonBalance?.walletAddress!!,
            address,
            amount.movePointRight(decimals).toBigInteger(),
            memo
        )
    }

    override suspend fun sendWithPayload(amount: BigInteger, address: String, payload: String)  {
        sendWithPayloadBoc(amount, address, payload)
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: FriendlyAddress,
        memo: String?,
    ): BigDecimal {
        val baseDecimals = nativeToken.decimals
        val estimateFee = tonKit.estimateFee(
            jettonWallet = jettonBalance?.walletAddress!!,
            recipient = address,
            amount = amount.movePointRight(decimals).toBigInteger(),
            comment = memo
        )

        return estimateFee.toBigDecimal(baseDecimals).stripTrailingZeros()
    }
}
