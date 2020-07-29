package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IUniswapKitManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.swap.SwapModule.CoinWithBalance
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
    val fromAmountErrorLiveData = MutableLiveData<Throwable>()
    val fromCoinLiveData = MutableLiveData<CoinWithBalance>()
    val toCoinLiveData = MutableLiveData<CoinWithBalance>()
    val tradeLiveData = MutableLiveData<TradeData>()
    val tradeTypeLiveData = MutableLiveData<TradeType>()

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

    private var tradeType: TradeType?
        get() = tradeTypeLiveData.value
        set(value) {
            tradeTypeLiveData.value = value
        }

    init {
        this.fromCoin = fromCoin?.let { coinWithBalance(it) }
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
        fromAmount = getNonZeroAmount(amount)
        toAmount = null

        tradeType = TradeType.ExactIn
        validateFromAmount(fromAmount)
        syncTradeData()
    }

    fun onToAmountChange(amount: String?) {
        toAmount = getNonZeroAmount(amount)
        fromAmount = null

        tradeType = TradeType.ExactOut
        syncTradeData()
    }

    private fun coinWithBalance(coin: Coin): CoinWithBalance {
        val balance = walletManager.wallet(coin)?.let { wallet ->
            adapterManager.getBalanceAdapterForWallet(wallet)?.balance
        }
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

    private fun syncTradeData() {
        val tradeData = swapData?.let { swapData ->
            try {
                when (tradeType) {
                    TradeType.ExactIn -> fromAmount?.let { kit.bestTradeExactIn(swapData, it) }
                    TradeType.ExactOut -> toAmount?.let { kit.bestTradeExactOut(swapData, it) }
                    else -> null
                }
            } catch (error: Throwable) {
                logger.info("bestTrade$tradeType error: ${error.javaClass.simpleName} (${error.localizedMessage})")
                null
            }
        }

        tradeData?.let {
            validateFromAmount(tradeData.amountIn)
        }

        tradeLiveData.value = tradeData
    }

    private fun validateFromAmount(fromAmount: BigDecimal?) {
        fromAmountErrorLiveData.value = fromAmount?.let {
            fromCoin?.balance?.let { balance ->
                if (fromAmount > balance) {
                    SwapModule.ValidationError.InsufficientBalance()
                } else
                    null

            }
        }
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
