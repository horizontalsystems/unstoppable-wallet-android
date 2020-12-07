package io.horizontalsystems.bankwallet.modules.swap.service

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.swap.SwapModule
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapError
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.SwapState
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.provider.AllowanceProvider
import io.horizontalsystems.bankwallet.modules.swap.repository.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class UniswapService(
        coinSending: Coin?,
        private val uniswapRepository: UniswapRepository,
        private val allowanceProvider: AllowanceProvider,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        private val transactionService: EthereumTransactionService,
        private val ethereumKit: EthereumKit,
        private val ethereumCoin: Coin
) : SwapModule.ISwapService, Clearable {
    private val priceImpactDesirableThreshold = BigDecimal("1")
    private val priceImpactAllowedThreshold = BigDecimal("5")

    private var tradeDisposable: Disposable? = null
    private var allowanceDisposable: Disposable? = null
    private var feeDisposable: Disposable? = null
    private var swapDisposable: Disposable? = null
    private var transactionServiceDisposable: Disposable? = null

    private val timer = Timer()
    private val allowanceRefreshInterval = 10_000L // milliseconds

    private var tradeData: DataState<TradeData?> = DataState.Success(null)
        set(value) {
            field = value
            trade = when (value) {
                is DataState.Success -> {
                    syncTransactionData(value.data)
                    DataState.Success(value.data?.let { trade(it) })
                }
                is DataState.Error -> value
                is DataState.Loading -> value
            }
        }

    override val defaultSwapSettings = SwapSettings(slippage = BigDecimal("0.5"), deadline = 20)

    override var currentSwapSettings = defaultSwapSettings

    override val coinSendingObservable = BehaviorSubject.create<Optional<Coin>>()
    override var coinSending: Coin? = coinSending
        private set(value) {
            field = value
            coinSendingObservable.onNext(Optional.ofNullable(value))
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
    override val balanceSending = BehaviorSubject.create<Optional<CoinValue>>()
    override val balanceReceiving = BehaviorSubject.create<Optional<CoinValue>>()
    override val allowance = BehaviorSubject.create<DataState<CoinValue?>>()
    override val errors = BehaviorSubject.create<List<SwapError>>()
    override val state = BehaviorSubject.createDefault<SwapState>(SwapState.Idle)

    override val swapFee: CoinValue?
        get() = trade.dataOrNull?.swapFee

    override val gasPriceType: EthereumTransactionService.GasPriceType
        get() = transactionService.gasPriceType

    override val transactionFee: BigInteger?
        get() = transactionService.transactionStatus.dataOrNull?.gasData?.fee

    private val ethereumBalance: BigInteger
        get() = ethereumKit.balance ?: BigInteger.ZERO

    init {
        coinSending?.let { enterCoinSending(it) }

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (state.value == SwapState.WaitingForApprove) {
                    syncAllowance()
                }
            }
        }, 1000, allowanceRefreshInterval)

        transactionServiceDisposable = transactionService.transactionStatusObservable
                .observeOn(Schedulers.io())
                .subscribe {
                    validateState()
                }
    }

    override fun enterCoinSending(coin: Coin) {
        coinSending = coin
        balanceSending.onNext(Optional.of(CoinValue(coin, balance(coin))))

        syncAllowance()
        syncTrade()
    }

    override fun enterCoinReceiving(coin: Coin) {
        coinReceiving = coin
        balanceReceiving.onNext(Optional.of(CoinValue(coin, balance(coin))))

        syncTrade()
    }

    override fun switchCoins() {
        val tmp = coinReceiving
        coinReceiving = coinSending
        coinSending = tmp

        balanceSending.onNext(Optional.ofNullable(coinSending?.let { CoinValue(it, balance(it)) }))
        balanceReceiving.onNext(Optional.ofNullable(coinReceiving?.let { CoinValue(it, balance(it)) }))

        syncAllowance()
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
        state.onNext(SwapState.SwapAllowed)
    }

    override fun cancelProceed() {
        state.onNext(SwapState.ProceedAllowed)
    }

    override fun approved() {
        state.onNext(SwapState.WaitingForApprove)
    }

    override fun updateSwapSettings(swapSettings: SwapSettings) {
        currentSwapSettings = swapSettings
        syncTrade()
    }

    override fun swap() {
        val tradeData = tradeData.dataOrNull
        val transaction = transactionService.transactionStatus.dataOrNull

        if (tradeData == null || transaction == null) {
            errors.onNext(listOf(SwapError.NotEnoughDataToSwap))
            return
        }

        state.onNext(SwapState.Swapping)
        swapDisposable = uniswapRepository.swap(tradeData, transaction.gasData.gasPrice, transaction.gasData.gasLimit)
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
        transactionServiceDisposable?.dispose()

        timer.cancel()
    }

    private fun syncTrade() {
        val amountType = amountType.value
        val amount = when (amountType) {
            AmountType.ExactSending -> amountSending
            AmountType.ExactReceiving -> amountReceiving
            else -> null
        }
        val coinSending = coinSending
        val coinReceiving = coinReceiving

        if (amountType == null || amount == null || amount.compareTo(BigDecimal.ZERO) == 0 || coinSending == null || coinReceiving == null) {
            tradeData = DataState.Success(null)
            validateState()
            return
        }

        tradeDisposable?.dispose()
        tradeDisposable = null

        tradeDisposable = uniswapRepository.getTradeData(coinSending, coinReceiving, amount, amountType, currentSwapSettings)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe {
                    tradeData = DataState.Loading
                }
                .doFinally {
                    validateState()
                }
                .subscribe({
                    tradeData = DataState.Success(it)
                }, {
                    tradeData = DataState.Error(it)
                })
    }

    private fun syncTransactionData(tradeData: TradeData?) {
        transactionService.transactionData = tradeData?.let {
            val ethereumTransactionData = uniswapRepository.transactionData(tradeData)
            EthereumTransactionService.TransactionData(
                    ethereumTransactionData.to,
                    ethereumTransactionData.value,
                    ethereumTransactionData.input,
            )
        }
    }

    private fun syncAllowance() {
        allowanceDisposable?.dispose()
        allowanceDisposable = null
        val coinSending = coinSending
        when (coinSending?.type) {
            is CoinType.Erc20 -> {
                allowanceDisposable = allowanceProvider.getAllowance(coinSending, uniswapRepository.routerAddress)
                        .subscribeOn(Schedulers.io())
                        .doOnSubscribe {
                            allowance.onNext(DataState.Loading)
                        }
                        .doFinally {
                            validateState()
                        }
                        .subscribe({
                            allowance.onNext(DataState.Success(CoinValue(coinSending, it)))
                        }, {
                            allowance.onNext(DataState.Error(it))
                        })
            }
            else -> {
                allowance.onNext(DataState.Success(null))
            }
        }
    }

    @Synchronized
    private fun validateState() {
        val newErrors = mutableListOf<SwapError>()
        val amountSending = amountSending
        val balanceSending = balanceSending.value?.let { if (it.isPresent) it.get().value else null }
        val allowanceDataState = allowance.value
        val tradeDataState = trade

        // validate balance
        if (amountSending != null && balanceSending != null && amountSending > balanceSending) {
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
            else
                amountSending

            val coinSending = coinSending
            if (coinSending != null && maxSendingAmount != null && allowanceData != null && maxSendingAmount > allowanceData.value) {
                val amount = maxSendingAmount.movePointRight(coinSending.decimal).toBigInteger()
                val allowance = allowanceData.value.movePointRight(coinSending.decimal).toBigInteger()

                newErrors.add(SwapError.InsufficientAllowance(SwapModule.ApproveData(coinSending, uniswapRepository.routerAddress.hex, amount, allowance)))
            }
        }

        // validate fee
        val txStatus = transactionService.transactionStatus
        when (txStatus) {
            is DataState.Success -> {
                txStatus.data.let { tx ->
                    if (tx.totalAmount > ethereumBalance) {
                        newErrors.add(SwapError.InsufficientBalanceForFee(CoinValue(ethereumCoin, BigDecimal(tx.totalAmount, ethereumCoin.decimal))))
                    }
                }
            }
            is DataState.Error -> {
                if (newErrors.isEmpty()) {
                    val rpcErrorMessage = (txStatus.error as? JsonRpc.ResponseError.RpcError)?.error?.message
                    when {
                        rpcErrorMessage?.contains("execution reverted") == true || rpcErrorMessage?.contains("gas required exceeds") == true -> {
                            newErrors.add(SwapError.InsufficientFeeCoinBalance)
                        }
                        txStatus.error !is EthereumTransactionService.GasDataError.NoTransactionData -> {
                            newErrors.add(SwapError.Other(txStatus.error))
                        }
                    }
                }
            }
        }

        // set new state
        val newState = when (val oldState = state.value ?: SwapState.Idle) {
            SwapState.Idle,
            is SwapState.ApproveRequired,
            SwapState.ProceedAllowed,
            SwapState.FetchingFee -> {
                when {
                    tradeDataState == DataState.Loading || allowanceDataState == DataState.Loading -> {
                        SwapState.Idle
                    }
                    txStatus == DataState.Loading -> {
                        SwapState.FetchingFee
                    }
                    tradeDataState is DataState.Success && tradeDataState.data != null -> {
                        if (newErrors.isEmpty()) {
                            SwapState.ProceedAllowed
                        } else if (newErrors.size == 1 && newErrors.first() is SwapError.InsufficientAllowance) {
                            val insufficientAllowanceError = newErrors.first { it is SwapError.InsufficientAllowance } as SwapError.InsufficientAllowance
                            SwapState.ApproveRequired(insufficientAllowanceError.approveData)
                        } else {
                            SwapState.Idle
                        }
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
                    tradeDataState is DataState.Success && tradeDataState.data != null -> {
                        if (newErrors.isEmpty())
                            SwapState.ProceedAllowed
                        else {
                            if (newErrors.any { it is SwapError.Other && it.error is JsonRpc.ResponseError.RpcError }) {
                                syncTrade()
                            }
                            SwapState.WaitingForApprove
                        }
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
        val coinReceiving = coinReceiving ?: return null
        val coinSending = coinSending ?: return null

        tradeData.apply {
            return Trade(
                    coinSending,
                    coinReceiving,
                    if (tradeData.type == TradeType.ExactIn) AmountType.ExactSending else AmountType.ExactReceiving,
                    amountIn,
                    amountOut,
                    executionPrice,
                    priceImpact(priceImpact),
                    tradeData.providerFee?.let { CoinValue(coinSending, it) },
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
