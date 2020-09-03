package io.horizontalsystems.bankwallet.modules.swap.service

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.DataState
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.repository.AllowanceRepository
import io.horizontalsystems.bankwallet.modules.swap.repository.UniswapRepository
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import java.util.*

class UniswapService(
        coinSending: Coin,
        private val uniswapRepository: UniswapRepository,
        private val allowanceRepository: AllowanceRepository,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val feeCoinProvider: FeeCoinProvider,
        private val uniswapFeeService: UniswapFeeService
) : SwapModule.ISwapService, Clearable {
    private val priceImpactDesirableThreshold = BigDecimal("1")
    private val priceImpactAllowedThreshold = BigDecimal("5")

    private var tradeDisposable: Disposable? = null
    private var allowanceDisposable: Disposable? = null
    private var feeDisposable: Disposable? = null
    private var swapDisposable: Disposable? = null

    private val timer = Timer()
    private val allowanceRefreshInterval = 10_000L // milliseconds

    private var tradeData: DataState<TradeData?> = DataState.Success(null)
        set(value) {
            field = value
            trade = when (value) {
                is DataState.Success -> DataState.Success(value.data?.let { trade(it) })
                is DataState.Error -> value
                is DataState.Loading -> value
            }
        }

    override val coinSendingObservable = BehaviorSubject.create<Coin>()
    override var coinSending: Coin = coinSending
        private set(value) {
            field = value
            coinSendingObservable.onNext(value)
        }

    override val coinReceivingObservable = BehaviorSubject.create<Optional<Coin>>()
    override var coinReceiving: Coin? = null
        private set(value) {
            field = value
            coinReceivingObservable.onNext(Optional.ofNullable(value))
        }

    override val amountSendingObservable = BehaviorSubject.create<Optional<BigDecimal>>()
    override var amountSending: BigDecimal? = null
        private set(value) {
            field = value
            amountSendingObservable.onNext(Optional.ofNullable(value))
        }

    override val amountReceivingObservable = BehaviorSubject.create<Optional<BigDecimal>>()
    override var amountReceiving: BigDecimal? = null
        private set(value) {
            field = value
            amountReceivingObservable.onNext(Optional.ofNullable(value))
        }

    override val tradeObservable = BehaviorSubject.create<DataState<Trade?>>()
    override var trade: DataState<Trade?> = DataState.Success(null)
        private set(value) {
            field = value
            if (value is DataState.Success) {
                when (value.data?.amountType) {
                    AmountType.ExactReceiving -> {
                        amountSending = value.data.amountSending
                    }
                    AmountType.ExactSending -> {
                        amountReceiving = value.data.amountReceiving
                    }
                }
            }
            tradeObservable.onNext(value)
        }

    override val amountType = BehaviorSubject.createDefault(AmountType.ExactSending)
    override val balance = BehaviorSubject.create<CoinValue>()
    override val allowance = BehaviorSubject.create<DataState<CoinValue?>>()
    override val errors = BehaviorSubject.create<List<SwapError>>()
    override val state = BehaviorSubject.createDefault<SwapState>(SwapState.Idle)
    override val fee = BehaviorSubject.create<DataState<SwapFeeInfo>>()

    override val swapFee: CoinValue?
        get() = amountSending?.multiply(BigDecimal("0.003"))?.let { CoinValue(coinSending, it) }

    override val feeRatePriority: FeeRatePriority
        get() = uniswapFeeService.feeRatePriority

    override val transactionFee: Pair<CoinValue, CurrencyValue?>?
        get() = fee.value?.dataOrNull?.let { Pair(it.coinAmount, it.fiatAmount) }

    init {
        enterCoinSending(coinSending)

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (state.value == SwapState.WaitingForApprove) {
                    syncAllowance()
                }
            }
        }, 1000, allowanceRefreshInterval)
    }

    override fun enterCoinSending(coin: Coin) {
        coinSending = coin
        balance.onNext(CoinValue(coin, balance(coin)))

        syncAllowance()
        syncTrade()
    }

    override fun enterCoinReceiving(coin: Coin) {
        coinReceiving = coin

        syncTrade()
    }

    override fun enterAmountSending(amount: BigDecimal?) {
        if (!amountsEqual(amount, amountSending)) {
            amountSending = amount
            amountReceiving = null
            amountType.onNext(AmountType.ExactSending)

            syncTrade()
        }
    }

    override fun enterAmountReceiving(amount: BigDecimal?) {
        if (!amountsEqual(amount, amountReceiving)) {
            amountReceiving = amount
            amountSending = null
            amountType.onNext(AmountType.ExactReceiving)

            syncTrade()
        }
    }

    override fun proceed() {
        check(state.value == SwapState.ProceedAllowed) {
            throw IllegalStateException("Cannot proceed when at state: ${state.value}")
        }
        state.onNext(SwapState.FetchingFee)
        syncFee()
    }

    override fun cancelProceed() {
        state.onNext(SwapState.ProceedAllowed)
    }

    override fun approved() {
        state.onNext(SwapState.WaitingForApprove)
    }

    override fun swap() {
        val tradeData = tradeData.dataOrNull
        val feeInfo = fee.value?.dataOrNull

        if (tradeData == null || feeInfo == null) {
            errors.onNext(listOf(SwapError.NotEnoughDataToSwap))
            return
        }

        state.onNext(SwapState.Swapping)
        swapDisposable = uniswapRepository.swap(tradeData, feeInfo.gasPrice, feeInfo.gasLimit)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    state.onNext(SwapState.Success)
                }, {
                    state.onNext(SwapState.Failed(SwapError.Other(it)))
                })
    }

    override fun clear() {
        tradeDisposable?.dispose()
        allowanceDisposable?.dispose()
        feeDisposable?.dispose()
        swapDisposable?.dispose()

        timer.cancel()
    }

    private fun syncTrade() {
        val amountType = amountType.value
        val amount = when (amountType) {
            AmountType.ExactSending -> amountSending
            AmountType.ExactReceiving -> amountReceiving
            else -> null
        }
        val coinReceiving = this.coinReceiving

        if (amountType == null || amount == null || amount.compareTo(BigDecimal.ZERO) == 0 || coinReceiving == null) {
            tradeData = DataState.Success(null)
            validateState()
            return
        }

        tradeDisposable?.dispose()
        tradeDisposable = null

        tradeData = DataState.Loading
        tradeDisposable = uniswapRepository.getTradeData(coinSending, coinReceiving, amount, amountType)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    tradeData = DataState.Success(it)
                    validateState()
                }, {
                    tradeData = DataState.Error(it)
                    validateState()
                })
    }

    private fun syncAllowance() {
        allowanceDisposable?.dispose()
        allowanceDisposable = null

        if (coinSending.type is CoinType.Erc20) {
            allowanceDisposable = allowanceRepository.allowance(coinSending)
                    .subscribeOn(Schedulers.io())
                    .map { dataState ->
                        when (dataState) {
                            is DataState.Success -> {
                                DataState.Success(CoinValue(coinSending, dataState.data))
                            }
                            is DataState.Error -> {
                                DataState.Error(dataState.error)
                            }
                            is DataState.Loading -> {
                                DataState.Loading
                            }
                        }
                    }
                    .subscribe {
                        allowance.onNext(it)
                        validateState()
                    }
        } else {
            allowance.onNext(DataState.Success(null))
        }
    }

    private fun syncFee() {
        val coinFee = feeCoinProvider.feeCoinData(coinSending)?.first ?: coinSending
        val tradeData = tradeData.dataOrNull ?: return

        feeDisposable?.dispose()
        feeDisposable = null

        feeDisposable = uniswapFeeService.swapFeeInfo(coinSending, coinFee, tradeData)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    fee.onNext(it)
                    validateState()
                }
    }

    @Synchronized
    private fun validateState() {
        val newErrors = mutableListOf<SwapError>()
        val amountSending = this.amountSending
        val balance = balance.value?.value
        val allowanceDataState = allowance.value
        val tradeDataState = trade
        val feeDataState = fee.value

        // validate balance
        if (amountSending != null && balance != null && amountSending > balance) {
            newErrors.add(SwapError.InsufficientBalance)
        }

        // validate tradeData
        if (tradeDataState is DataState.Error) {
            val swapError = when (tradeDataState.error) {
                is TradeError.TradeNotFound -> SwapError.NoLiquidity
                else -> SwapError.CouldNotFetchTrade
            }
            newErrors.add(swapError)
        } else if (tradeDataState is DataState.Success) {
            val tradeData = tradeDataState.data
            if (tradeData != null && tradeData.priceImpact?.level == PriceImpact.Level.Forbidden) {
                newErrors.add(SwapError.TooHighPriceImpact)
            }
        }

        // validate allowance
        if (allowanceDataState is DataState.Error) {
            newErrors.add(SwapError.CouldNotFetchAllowance)
        } else if (allowanceDataState is DataState.Success) {
            val allowanceData = allowanceDataState.data
            val tradeData = tradeDataState.dataOrNull

            val maxSendingAmount = if (tradeData != null && tradeData.amountType == AmountType.ExactReceiving)
                tradeData.minMaxAmount
            else amountSending

            if (maxSendingAmount != null && allowanceData != null && maxSendingAmount > allowanceData.value) {
                newErrors.add(SwapError.InsufficientAllowance(SwapModule.ApproveData(coinSending, maxSendingAmount, uniswapRepository.routerAddress.hex)))
            }
        }

        // validate fee
        if (feeDataState is DataState.Error) {
            newErrors.add(SwapError.CouldNotFetchFee)
        } else if (feeDataState is DataState.Success) {
            val fee = feeDataState.data.coinAmount.value
            val feeCoin = feeCoinProvider.feeCoinData(coinSending)?.first ?: coinSending
            val feeCoinBalance = balance(feeCoin)
            val feeCoinSpendingAmount = if (coinSending.type == feeCoin.type) amountSending else BigDecimal.ZERO

            if (amountSending != null && fee > feeCoinBalance.subtract(feeCoinSpendingAmount)) {
                newErrors.add(SwapError.InsufficientBalanceForFee)
            }
        }

        // set new state
        val newState = when (val oldState = state.value ?: SwapState.Idle) {
            SwapState.Idle,
            is SwapState.ApproveRequired,
            SwapState.ProceedAllowed -> {
                when {
                    tradeDataState == DataState.Loading || allowanceDataState == DataState.Loading -> {
                        SwapState.Idle
                    }
                    feeDataState == DataState.Loading -> {
                        SwapState.FetchingFee
                    }
                    newErrors.size == 1 && newErrors.first() is SwapError.InsufficientAllowance -> {
                        val insufficientAllowanceError = newErrors.first { it is SwapError.InsufficientAllowance } as SwapError.InsufficientAllowance
                        SwapState.ApproveRequired(insufficientAllowanceError.approveData)
                    }
                    tradeDataState is DataState.Success && tradeDataState.data != null && newErrors.isEmpty() -> {
                        SwapState.ProceedAllowed
                    }
                    else -> {
                        SwapState.Idle
                    }
                }
            }
            SwapState.WaitingForApprove -> {
                when {
                    tradeDataState == DataState.Loading -> SwapState.Idle
                    allowanceDataState == DataState.Loading || newErrors.size == 1 && newErrors.first() is SwapError.InsufficientAllowance -> {
                        SwapState.WaitingForApprove
                    }
                    tradeDataState is DataState.Success && tradeDataState.data != null && newErrors.isEmpty() -> {
                        SwapState.ProceedAllowed
                    }
                    else -> {
                        SwapState.Idle
                    }
                }
            }
            SwapState.FetchingFee -> {
                when {
                    feeDataState == DataState.Loading -> {
                        SwapState.FetchingFee
                    }
                    feeDataState is DataState.Error -> {
                        SwapState.ProceedAllowed
                    }
                    feeDataState is DataState.Success && newErrors.isEmpty() -> {
                        SwapState.SwapAllowed
                    }
                    else -> {
                        SwapState.Idle
                    }
                }
            }
            else -> oldState
        }

        state.onNext(newState)
        errors.onNext(newErrors)
    }

    private fun balance(coin: Coin?): BigDecimal {
        val wallet = coin?.let { walletManager.wallet(it) }
        val balanceAdapter = wallet?.let { adapterManager.getBalanceAdapterForWallet(it) }
        return balanceAdapter?.balance ?: BigDecimal.ZERO
    }

    private fun trade(tradeData: TradeData): Trade? {
        val coinReceiving = this.coinReceiving ?: return null
        tradeData.apply {
            return Trade(
                    coinSending,
                    coinReceiving,
                    if (tradeData.type == TradeType.ExactIn) AmountType.ExactSending else AmountType.ExactReceiving,
                    amountIn,
                    amountOut,
                    executionPrice,
                    priceImpact(priceImpact),
                    if (tradeData.type == TradeType.ExactIn) amountOutMin else amountInMax
            )
        }
    }

    private fun priceImpact(value: BigDecimal?): PriceImpact? {
        return value?.let {
            when {
                value < priceImpactDesirableThreshold -> PriceImpact(value, PriceImpact.Level.Normal)
                value < priceImpactAllowedThreshold -> PriceImpact(value, PriceImpact.Level.Warning)
                else -> PriceImpact(value, PriceImpact.Level.Forbidden)
            }
        }
    }

    private fun amountsEqual(amount1: BigDecimal?, amount2: BigDecimal?): Boolean {
        return when {
            amount1 == null && amount2 == null -> true
            amount1 != null && amount2 != null && amount2.compareTo(amount1) == 0 -> true
            else -> false
        }
    }

}
