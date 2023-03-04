package io.horizontalsystems.bankwallet.modules.swap.uniswap

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapPendingAllowanceService
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.util.*

class UniswapService(
        private val dex: SwapMainModule.Dex,
        private val tradeService: UniswapTradeService,
        val allowanceService: SwapAllowanceService,
        val pendingAllowanceService: SwapPendingAllowanceService,
        private val adapterManager: IAdapterManager
) : SwapMainModule.ISwapService {
    private val disposables = CompositeDisposable()

    //region internal subjects
    private val stateSubject = PublishSubject.create<State>()
    private val errorsSubject = PublishSubject.create<List<Throwable>>()
    private val balanceFromSubject = PublishSubject.create<Optional<BigDecimal>>()
    private val balanceToSubject = PublishSubject.create<Optional<BigDecimal>>()
    //endregion

    //region outputs
    var state: State = State.NotReady
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateObservable: Observable<State> = stateSubject

    override var errors: List<Throwable> = listOf()
        private set(value) {
            field = value
            errorsSubject.onNext(value)
        }
    override val errorsObservable: Observable<List<Throwable>> = errorsSubject

    override var balanceFrom: BigDecimal? = null
        private set(value) {
            field = value
            balanceFromSubject.onNext(Optional.ofNullable(value))
        }
    override val balanceFromObservable: Observable<Optional<BigDecimal>> = balanceFromSubject

    override var balanceTo: BigDecimal? = null
        private set(value) {
            field = value
            balanceToSubject.onNext(Optional.ofNullable(value))
        }
    override val balanceToObservable: Observable<Optional<BigDecimal>> = balanceToSubject

    val blockchainType = dex.blockchainType
    val revokeEvmData : SendEvmData?
        get() = allowanceService.revokeEvmData()

    val approveData: SwapAllowanceService.ApproveData?
        get() = balanceFrom?.let { amount ->
            allowanceService.approveData(dex, amount)
        }
    //endregion

    init {
        tradeService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateTrade()
                }
                .let { disposables.add(it) }

        tradeService.tokenFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe { token ->
                    onUpdateCoinFrom(token.orElse(null))
                }
                .let { disposables.add(it) }
        onUpdateCoinFrom(tradeService.tokenFrom)

        tradeService.tokenToObservable
                .subscribeOn(Schedulers.io())
                .subscribe { coin ->
                    onUpdateCoinTo(coin.orElse(null))
                }
                .let { disposables.add(it) }

        tradeService.amountFromObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateAmountFrom()
                }
                .let { disposables.add(it) }

        allowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    syncState()
                }
                .let { disposables.add(it) }

        pendingAllowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateAllowancePending()
                }
                .let { disposables.add(it) }
    }

    override fun start() {
        allowanceService.start()
        tradeService.start()
    }

    override fun stop() {
        allowanceService.stop()
        tradeService.stop()
    }

    fun onCleared() {
        disposables.clear()
        tradeService.onCleared()
        allowanceService.onCleared()
        pendingAllowanceService.onCleared()
    }

    private fun onUpdateTrade() {
        syncState()
    }

    private fun onUpdateCoinFrom(token: Token?) {
        balanceFrom = token?.let { balance(it) }
        allowanceService.set(token)
        pendingAllowanceService.set(token)
    }

    private fun onUpdateCoinTo(token: Token?) {
        balanceTo = token?.let { balance(it) }
    }

    private fun onUpdateAmountFrom() {
        syncState()
    }

    private fun onUpdateAllowancePending() {
        syncState()
    }

    private fun syncState() {
        val allErrors = mutableListOf<Throwable>()
        var loading = false
        var transactionData: TransactionData? = null

        when (val state = tradeService.state) {
            UniswapTradeService.State.Loading -> {
                loading = true
            }
            is UniswapTradeService.State.Ready -> {
                transactionData = try {
                    tradeService.transactionData(state.trade.tradeData)
                } catch (error: Throwable) {
                    null
                }
            }
            is UniswapTradeService.State.NotReady -> {
                allErrors.addAll(state.errors)
            }
        }

        when (val state = allowanceService.state) {
            SwapAllowanceService.State.Loading -> {
                loading = true
            }
            is SwapAllowanceService.State.Ready -> {
                tradeService.amountFrom?.let { amountFrom ->
                    if (amountFrom > state.allowance.value) {
                        if (revokeRequired()) {
                            allErrors.add(SwapError.RevokeAllowanceRequired)
                        } else {
                            allErrors.add(SwapError.InsufficientAllowance)
                        }
                    }
                }
            }
            is SwapAllowanceService.State.NotReady -> {
                allErrors.add(state.error)
            }
            null -> {}
        }

        tradeService.amountFrom?.let { amountFrom ->
            val balanceFrom = balanceFrom
            if (balanceFrom == null || balanceFrom < amountFrom) {
                allErrors.add(SwapError.InsufficientBalanceFrom)
            }
        }

        if (pendingAllowanceService.state.loading()) {
            loading = true
        }

        if (!loading) {
            errors = allErrors
        }

        state = when {
            loading -> State.Loading
            errors.isEmpty() && transactionData != null -> State.Ready(transactionData)
            else -> State.NotReady
        }
    }

    private fun revokeRequired(): Boolean {
        val tokenFrom = tradeService.tokenFrom ?: return false
        val allowance = approveData?.allowance ?: return false

        return allowance.compareTo(BigDecimal.ZERO) != 0 && isUsdt(tokenFrom)
    }

    private fun isUsdt(token: Token): Boolean {
        val tokenType = token.type

        return token.blockchainType is BlockchainType.Ethereum
            && tokenType is TokenType.Eip20
            && tokenType.address.lowercase() == "0xdac17f958d2ee523a2206206994597c13d831ec7"
    }

    private fun balance(coin: Token): BigDecimal? =
            (adapterManager.getAdapterForToken(coin) as? IBalanceAdapter)?.balanceData?.available

    //region models
    sealed class State {
        object Loading : State()
        class Ready(val transactionData: TransactionData) : State()
        object NotReady : State()
    }
    //endregion

}
