package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IUniswapKitManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.CoinWithBalance
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.ValidationError.*
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.uniswapkit.models.SwapData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.models.TradeType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.logging.Logger

class SwapViewModel(
        private val uniswapKitManager: IUniswapKitManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager,
        fromCoin: Coin? = null
) : ViewModel() {

    private val logger = Logger.getLogger("SwapViewModel")
    private val disposables = CompositeDisposable()

    private val kit = uniswapKitManager.uniswapKit()

    private var swapData: SwapData? = null
    private var fromAmount: BigDecimal? = null
    private var toAmount: BigDecimal? = null

    val priceImpactDesirableThreshold = BigDecimal("1")
    val priceImpactAllowedThreshold = BigDecimal("5")

    val fromAmountLiveData = MutableLiveData<BigDecimal>()
    val fromCoinLiveData = MutableLiveData<CoinWithBalance>()
    val toCoinLiveData = MutableLiveData<CoinWithBalance>()
    val tradeLiveData = MutableLiveData<TradeData>()
    val tradeTypeLiveData = MutableLiveData<TradeType>()
    val proceedButtonEnabledLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Throwable>()

    private var fromCoin: CoinWithBalance?
        get() = fromCoinLiveData.value
        set(value) {
            fromCoinLiveData.value = value
        }

    private var toCoin: CoinWithBalance?
        get() = toCoinLiveData.value
        set(value) {
            toCoinLiveData.value = value
        }

    private var tradeType: TradeType
        get() = tradeTypeLiveData.value ?: TradeType.ExactIn
        set(value) {
            tradeTypeLiveData.value = value
        }

    private var tradeData: TradeData?
        get() = tradeLiveData.value
        set(value) {
            tradeLiveData.value = value
        }

    private var error: Throwable?
        get() = errorLiveData.value
        set(value) {
            errorLiveData.value = value
            proceedButtonEnabledLiveData.value = canProceed()
        }

    init {
        this.fromCoin = fromCoin?.let { coinWithBalance(it) }
        tradeType = TradeType.ExactIn
    }

    fun onSelectFromCoin(selectedCoin: Coin) {
        fromCoin = coinWithBalance(selectedCoin)

        syncSwapData()
    }

    fun onSelectToCoin(selectedCoin: Coin) {
        toCoin = coinWithBalance(selectedCoin)

        syncSwapData()
    }

    fun onFromAmountMaxButtonClick() {
        fromAmountLiveData.value = fromCoin?.balance
    }

    fun onFromAmountChange(amount: String?) {
        fromAmount = getNonZeroAmount(amount).apply {
            error = validateFromAmount(this)
        }
        toAmount = null

        tradeType = TradeType.ExactIn
        syncTradeData()
    }

    fun onToAmountChange(amount: String?) {
        toAmount = getNonZeroAmount(amount)
        fromAmount = null

        tradeType = TradeType.ExactOut
        syncTradeData()
    }

    fun onProceedButtonClick() {
        //todo
    }

    private fun coinWithBalance(coin: Coin): CoinWithBalance {
        val wallet = walletManager.wallet(coin)
        val balanceAdapter = wallet?.let { adapterManager.getBalanceAdapterForWallet(it) }
        val balance = balanceAdapter?.balance ?: BigDecimal.ZERO
        return CoinWithBalance(coin, balance ?: BigDecimal.ZERO)
    }

    private fun getNonZeroAmount(amount: String?): BigDecimal? {
        return if (amount?.trimEnd { it == '0' || it == '.' }.isNullOrBlank()) null else BigDecimal(amount)
    }

    private fun syncSwapData() {
        swapData = null

        val tokenIn = fromCoin?.let { uniswapToken(it.coin) } ?: return
        val tokenOut = toCoin?.let { uniswapToken(it.coin) } ?: return

        kit.swapData(tokenIn, tokenOut)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    logger.info("swapData: $it")
                    swapData = it
                    syncTradeData()
                }, {
                    logger.warning("swapData error: ${it.message}")
                }).let {
                    disposables.add(it)
                }
    }

    private fun canProceed(): Boolean {
        val priceImpact = tradeData?.priceImpact

        return error != null &&
                tradeData != null &&
                priceImpact != null && priceImpact < priceImpactAllowedThreshold
    }

    private fun syncTradeData() {
        val swapData = swapData ?: return
        try {
            tradeData = when (tradeType) {
                TradeType.ExactIn -> fromAmount?.let { kit.bestTradeExactIn(swapData, it) }
                TradeType.ExactOut -> toAmount?.let { kit.bestTradeExactOut(swapData, it) }
            }
            error = validateTradeData(tradeData)
        } catch (throwable: Throwable) {
            logger.info("bestTrade$tradeType error: ${throwable.javaClass.simpleName} (${throwable.localizedMessage})")
            error = throwable
            tradeData = null
        }
    }

    private fun validateTradeData(tradeData: TradeData?): Throwable? {
        if (tradeData == null) {
            return NoTradeData()
        }
        validateFromAmount(tradeData.amountIn)?.let { fromAmountError ->
            return fromAmountError
        }
        val priceImpact = tradeData.priceImpact ?: return PriceImpactInvalid()
        if (priceImpact < priceImpactAllowedThreshold) {
            return PriceImpactTooHigh()
        }
        return null
    }

    private fun validateFromAmount(fromAmount: BigDecimal?): Throwable? {
        val fromAmount = fromAmount ?: return null
        val balance = fromCoin?.balance ?: return null

        return if (fromAmount > balance)
            InsufficientBalance()
        else
            null
    }

    private fun uniswapToken(coin: Coin): Token? {
        return when (val coinType = coin.type) {
            is CoinType.Ethereum -> kit.etherToken()
            is CoinType.Erc20 -> {
                kit.token(coinType.address.hexStringToByteArray(), coin.decimal)
            }
            else -> null
        }
    }

    override fun onCleared() {
        uniswapKitManager.unlink()
    }

}
