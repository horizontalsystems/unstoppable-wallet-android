package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EthereumKitNotCreated
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.repository.AllowanceRepository
import io.horizontalsystems.bankwallet.modules.swap.repository.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap.service.SwapFeeInfo
import io.horizontalsystems.bankwallet.modules.swap.service.UniswapFeeService
import io.horizontalsystems.bankwallet.modules.swap.service.UniswapService
import io.horizontalsystems.bankwallet.modules.swap.view.SwapActivity
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

object SwapModule {
    const val tokenInKey = "tokenInKey"

    interface ISwapService {
        val coinSending: Coin
        val coinSendingObservable: Observable<Coin>

        val coinReceiving: Coin?
        val coinReceivingObservable: Observable<Optional<Coin>>

        val amountSending: BigDecimal?
        val amountSendingObservable: Observable<Optional<BigDecimal>>

        val amountReceiving: BigDecimal?
        val amountReceivingObservable: Observable<Optional<BigDecimal>>

        val trade: DataState<Trade?>?
        val tradeObservable: Observable<DataState<Trade?>>

        val amountType: Observable<AmountType>
        val balance: Observable<CoinValue>
        val allowance: Observable<DataState<CoinValue?>>
        val errors: Observable<List<SwapError>>
        val state: Observable<SwapState>
        val fee: Observable<DataState<SwapFeeInfo>>

        val swapFee: CoinValue?
        val feeRatePriority: FeeRatePriority
        val transactionFee: Pair<CoinValue, CurrencyValue?>?

        fun enterCoinSending(coin: Coin)
        fun enterCoinReceiving(coin: Coin)
        fun enterAmountSending(amount: BigDecimal?)
        fun enterAmountReceiving(amount: BigDecimal?)
        fun proceed()
        fun cancelProceed()
        fun swap()
        fun approved()
    }

    sealed class SwapError {
        object InsufficientBalance : SwapError()
        class InsufficientAllowance(val approveData: ApproveData) : SwapError()
        object InsufficientBalanceForFee : SwapError()
        object TooHighPriceImpact : SwapError()
        object NoLiquidity : SwapError()
        object CouldNotFetchTrade : SwapError()
        object CouldNotFetchAllowance : SwapError()
        object CouldNotFetchFee : SwapError()
        object NotEnoughDataToSwap : SwapError()
        class Other(val error: Throwable) : SwapError()
    }

    data class ApproveData(val coin: Coin, val amount: BigDecimal, val spenderAddress: String)

    sealed class SwapState {
        object Idle : SwapState()
        class ApproveRequired(val data: ApproveData) : SwapState()
        object WaitingForApprove : SwapState()
        object ProceedAllowed : SwapState()
        object FetchingFee : SwapState()
        object SwapAllowed : SwapState()
        object Swapping : SwapState()
        class Failed(val error: SwapError) : SwapState()
        object Success : SwapState()
    }

    fun start(context: Context, tokenIn: Coin) {
        val intent = Intent(context, SwapActivity::class.java)
        intent.putExtra(tokenInKey, tokenIn)

        context.startActivity(intent)
    }

    class Factory(private val coinSending: Coin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ethereumKit = App.ethereumKitManager.ethereumKit ?: throw EthereumKitNotCreated()
            val uniswapKit = UniswapKit.getInstance(ethereumKit)
            val swapRepository = UniswapRepository(uniswapKit)
            val allowanceRepository = AllowanceRepository(ethereumKit, ethereumKit.receiveAddress, uniswapKit.routerAddress)
            val feeRateProvider = FeeRateProviderFactory.provider(coinSending)
            val uniswapFeeService = UniswapFeeService(uniswapKit, App.walletManager, App.adapterManager, App.currencyManager.baseCurrency, App.xRateManager, feeRateProvider!!)
            val swapService = UniswapService(coinSending, swapRepository, allowanceRepository, App.walletManager, App.adapterManager, App.feeCoinProvider, uniswapFeeService)
            val resourceProvider = ResourceProvider(App.instance)

            return SwapViewModel(swapService, resourceProvider, App.numberFormatter) as T
        }
    }
}
