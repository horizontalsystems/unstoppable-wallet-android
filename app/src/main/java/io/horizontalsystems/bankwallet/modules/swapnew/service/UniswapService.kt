package io.horizontalsystems.bankwallet.modules.swapnew.service

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swapnew.DataState
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew.SwapError
import io.horizontalsystems.bankwallet.modules.swapnew.SwapModuleNew.SwapState
import io.horizontalsystems.bankwallet.modules.swapnew.model.AmountType
import io.horizontalsystems.bankwallet.modules.swapnew.model.PriceImpact
import io.horizontalsystems.bankwallet.modules.swapnew.model.Trade
import io.horizontalsystems.bankwallet.modules.swapnew.repository.AllowanceRepository
import io.horizontalsystems.bankwallet.modules.swapnew.repository.UniswapRepository
import io.horizontalsystems.uniswapkit.TradeError
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class UniswapService(
        coinSending: Coin,
        private val uniswapRepository: UniswapRepository,
        private val allowanceRepository: AllowanceRepository,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager
) : SwapModuleNew.ISwapService {
    private var tradeDisposable: Disposable? = null
    private var allowanceDisposable: Disposable? = null

    override val coinSending = BehaviorSubject.create<Coin>()
    override val coinReceiving = BehaviorSubject.create<Coin>()
    override val amountSending = BehaviorSubject.create<BigDecimal>()
    override val amountReceiving = BehaviorSubject.create<BigDecimal>()
    override val amountType = BehaviorSubject.createDefault(AmountType.ExactSending)
    override val balance = BehaviorSubject.create<CoinValue>()
    override val allowance = BehaviorSubject.create<DataState<CoinValue?>>()
    override val trade = BehaviorSubject.create<DataState<Trade?>>()
    override val errors = BehaviorSubject.create<List<SwapError>>()
    override val state = BehaviorSubject.createDefault<SwapState>(SwapState.Idle)

    init {
        setCoinSending(coinSending)
    }

    override fun setCoinSending(coin: Coin) {
        coinSending.onNext(coin)
        balance.onNext(CoinValue(coin, getBalance(coin)))

        syncAllowance()
        syncTrade()
    }

    override fun setCoinReceiving(coin: Coin) {
        coinReceiving.onNext(coin)

        syncTrade()
    }

    override fun setAmountSending(amount: BigDecimal) {
        if (amount != amountSending.value) {
            amountSending.onNext(amount)
            amountReceiving.onNext(BigDecimal.ZERO)
            amountType.onNext(AmountType.ExactSending)

            syncTrade()
        }
    }

    override fun setAmountReceiving(amount: BigDecimal) {
        if (amount != amountReceiving.value) {
            amountReceiving.onNext(amount)
            amountSending.onNext(BigDecimal.ZERO)
            amountType.onNext(AmountType.ExactReceiving)

            syncTrade()
        }
    }

    private fun syncTrade() {
        val amountType = amountType.value
        val amount = when (amountType) {
            AmountType.ExactSending -> amountSending.value
            AmountType.ExactReceiving -> amountReceiving.value
            else -> null
        }

        val coinSending = coinSending.value
        val coinReceiving = coinReceiving.value

        if (amountType == null || amount == null || amount == BigDecimal.ZERO || coinSending == null || coinReceiving == null) {
            trade.onNext(DataState.Success(null))
            validateState()
            return
        }

        tradeDisposable?.dispose()
        tradeDisposable = null

        tradeDisposable = uniswapRepository.trade(coinSending, coinReceiving, amount, amountType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { dataState ->
                    if (dataState is DataState.Success) {
                        when (dataState.data.amountType) {
                            AmountType.ExactReceiving -> {
                                amountSending.onNext(dataState.data.amountSending
                                        ?: BigDecimal.ZERO)
                            }
                            AmountType.ExactSending -> {
                                amountReceiving.onNext(dataState.data.amountReceiving
                                        ?: BigDecimal.ZERO)
                            }
                        }
                    }
                    trade.onNext(dataState)
                    validateState()
                }
    }

    private fun syncAllowance() {
        val coin = coinSending.value
        if (coin?.type is CoinType.Erc20) {
            allowanceDisposable?.dispose()
            allowanceDisposable = null

            allowanceDisposable = allowanceRepository.allowance(coin)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { allowanceDataState ->
                        when (allowanceDataState) {
                            is DataState.Success -> {
                                DataState.Success(CoinValue(coin, allowanceDataState.data))
                            }
                            is DataState.Error -> {
                                DataState.Error(allowanceDataState.error)
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

    @Synchronized
    private fun validateState() {
        val newErrors = mutableListOf<SwapError>()
        val amountSending = amountSending.value
        val allowanceData = allowance.value?.dataOrNull?.value
        val tradeData = trade.value?.dataOrNull

        if (amountSending != null && amountSending > balance.value?.value) {
            newErrors.add(SwapError.InsufficientBalance)
        }
        if (tradeData != null && tradeData.priceImpact?.level == PriceImpact.Level.Forbidden) {
            newErrors.add(SwapError.TooHighPriceImpact)
        }
        if ((trade.value as? DataState.Error)?.error is TradeError.TradeNotFound) {
            newErrors.add(SwapError.NoLiquidity)
        }
        if (amountSending != null && allowanceData != null && amountSending > allowanceData) {
            newErrors.add(SwapError.InsufficientAllowance)
        }

        val newState = when {
            trade.value is DataState.Loading || allowance.value is DataState.Loading -> {
                SwapState.Idle
            }
            tradeData != null && newErrors.size == 1 && newErrors[0] == SwapError.InsufficientAllowance -> {
                SwapState.ApproveRequired
            }
            tradeData != null && newErrors.size == 0 -> {
                SwapState.SwapAllowed
            }
            else -> {
                SwapState.Idle
            }
        }

        state.onNext(newState)
        errors.onNext(newErrors)
    }

    private fun getBalance(coin: Coin): BigDecimal {
        val wallet = walletManager.wallet(coin)
        val balanceAdapter = wallet?.let { adapterManager.getBalanceAdapterForWallet(it) }
        return balanceAdapter?.balance ?: BigDecimal.ZERO
    }

}
