package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EthereumKitNotCreated
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.confirmation.ConfirmationPresenter
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.provider.AllowanceProvider
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.provider.SwapFeeInfo
import io.horizontalsystems.bankwallet.modules.swap.provider.UniswapFeeProvider
import io.horizontalsystems.bankwallet.modules.swap.repository.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap.service.UniswapService
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import java.math.BigDecimal
import java.util.*

object SwapModule {

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
        val fee: Observable<DataState<SwapFeeInfo?>>

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
        class InsufficientBalanceForFee(val coinValue: CoinValue) : SwapError()
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

    class Factory(private val coinSending: Coin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val ethereumKit = App.ethereumKitManager.ethereumKit ?: throw EthereumKitNotCreated()
            val uniswapKit = UniswapKit.getInstance(ethereumKit)

            val allowanceProvider = AllowanceProvider(App.adapterManager)
            val feeRateProvider = FeeRateProviderFactory.provider(coinSending)
            val uniswapFeeProvider = UniswapFeeProvider(uniswapKit, App.walletManager, App.adapterManager, App.currencyManager.baseCurrency, App.xRateManager, feeRateProvider!!)
            val stringProvider = StringProvider(App.instance)

            val swapRepository = UniswapRepository(uniswapKit)
            val swapService = UniswapService(coinSending, swapRepository, allowanceProvider, App.walletManager, App.adapterManager, App.feeCoinProvider, uniswapFeeProvider)
            val formatter = SwapItemFormatter(stringProvider, App.numberFormatter)
            val confirmationPresenter = ConfirmationPresenter(swapService, stringProvider, formatter)

            return SwapViewModel(confirmationPresenter, swapService, stringProvider, formatter, listOf(swapService, confirmationPresenter)) as T
        }
    }
}
