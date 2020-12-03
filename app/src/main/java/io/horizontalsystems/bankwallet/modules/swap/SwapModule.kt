package io.horizontalsystems.bankwallet.modules.swap

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.CoinService
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EthereumTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.core.providers.EthereumFeeRateProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.swap.confirmation.ConfirmationPresenter
import io.horizontalsystems.bankwallet.modules.swap.model.AmountType
import io.horizontalsystems.bankwallet.modules.swap.model.Trade
import io.horizontalsystems.bankwallet.modules.swap.provider.AllowanceProvider
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.repository.UniswapRepository
import io.horizontalsystems.bankwallet.modules.swap.service.UniswapService
import io.horizontalsystems.bankwallet.modules.swap.settings.SwapSettingsModule.SwapSettings
import io.horizontalsystems.bankwallet.modules.swap_new.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.view.SwapViewModel
import io.horizontalsystems.uniswapkit.UniswapKit
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

object SwapModule {

    interface ISwapService {
        val coinSending: Coin?
        val coinSendingObservable: Observable<Optional<Coin>>

        val coinReceiving: Coin?
        val coinReceivingObservable: Observable<Optional<Coin>>

        val amountSending: BigDecimal?
        val amountSendingObservable: Observable<Optional<BigDecimal>>

        val amountReceiving: BigDecimal?
        val amountReceivingObservable: Observable<Optional<BigDecimal>>

        val trade: DataState<Trade?>?
        val tradeObservable: Observable<DataState<Trade?>>

        val amountType: Observable<AmountType>
        val balanceSending: Observable<Optional<CoinValue>>
        val balanceReceiving: Observable<Optional<CoinValue>>
        val allowance: Observable<DataState<CoinValue?>>
        val errors: Observable<List<SwapError>>
        val state: Observable<SwapState>

        val swapFee: CoinValue?
        val gasPriceType: EthereumTransactionService.GasPriceType
        val transactionFee: BigInteger?

        val defaultSwapSettings: SwapSettings
        val currentSwapSettings: SwapSettings

        fun enterCoinSending(coin: Coin)
        fun enterCoinReceiving(coin: Coin)
        fun switchCoins()
        fun enterAmountSending(amount: BigDecimal?)
        fun enterAmountReceiving(amount: BigDecimal?)
        fun proceed()
        fun cancelProceed()
        fun swap()
        fun approved()
        fun updateSwapSettings(swapSettings: SwapSettings)
    }

    sealed class SwapError {
        object InsufficientBalance : SwapError()
        class InsufficientAllowance(val approveData: ApproveData) : SwapError()
        class InsufficientBalanceForFee(val coinValue: CoinValue) : SwapError()
        object InsufficientFeeCoinBalance : SwapError()
        object TooHighPriceImpact : SwapError()
        object NoLiquidity : SwapError()
        object CouldNotFetchTrade : SwapError()
        object CouldNotFetchAllowance : SwapError()
        object CouldNotFetchFee : SwapError()
        object NotEnoughDataToSwap : SwapError()
        class Other(val error: Throwable) : SwapError()
    }

    @Parcelize
    data class ApproveData(
            val coin: Coin,
            val spenderAddress: String,
            val amount: BigInteger,
            val allowance: BigInteger
    ) : Parcelable

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

    class Factory(private val coinSending: Coin?) : ViewModelProvider.Factory {
        private val ethereumKit by lazy { App.ethereumKitManager.ethereumKit!! }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(App.appConfigProvider.ethereumCoin) as EthereumFeeRateProvider
            EthereumTransactionService(ethereumKit, feeRateProvider)
        }
        private val ethCoinService by lazy { CoinService(App.appConfigProvider.ethereumCoin, App.currencyManager, App.xRateManager) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                SwapViewModel::class.java -> {
                    val uniswapKit = UniswapKit.getInstance(ethereumKit)

                    val allowanceProvider = AllowanceProvider(App.adapterManager)
                    val feeRateProvider = EthereumFeeRateProvider(App.feeRateProvider)
                    val stringProvider = StringProvider(App.instance)

                    val swapRepository = UniswapRepository(uniswapKit)
                    val swapService = UniswapService(coinSending, swapRepository, allowanceProvider, App.walletManager, App.adapterManager, transactionService, ethereumKit, App.appConfigProvider.ethereumCoin)
                    val formatter = SwapViewItemHelper(stringProvider, App.numberFormatter)
                    val confirmationPresenter = ConfirmationPresenter(swapService, stringProvider, formatter, ethCoinService)

                    return SwapViewModel(confirmationPresenter, swapService, stringProvider, formatter, listOf(swapService, confirmationPresenter)) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, ethCoinService) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
