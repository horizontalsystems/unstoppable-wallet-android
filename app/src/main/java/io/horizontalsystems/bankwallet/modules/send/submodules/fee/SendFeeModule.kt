package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModule
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal


object SendFeeModule {

    class InsufficientFeeBalance(val coin: PlatformCoin, val coinProtocol: String, val feeCoin: PlatformCoin, val fee: CoinValue) :
            Exception()

    interface IFeeModule {
        val isValid: Boolean
        val feeRateState: FeeRateState
        val feeRate: Long?
        val coinValue: CoinValue
        val currencyValue: CurrencyValue?

        fun setLoading(loading: Boolean)
        fun setFee(fee: BigDecimal)
        fun setError(externalError: Exception?)
        fun setAvailableFeeBalance(availableFeeBalance: BigDecimal)
        fun setInputType(inputType: AmountInputModule.InputType)
        fun fetchFeeRate()
        fun setBalance(balance: BigDecimal)
        fun setRate(rate: BigDecimal?)
        fun setAmountInfo(sendAmountInfo: SendAmountInfo)
    }

    interface IFeeModuleDelegate {
        fun onUpdateFeeRate()
    }

    data class FeeRateInfoViewItem(val feeRatePriority: FeeRatePriority, val selected: Boolean)


    class Factory(
            private val coin: PlatformCoin,
            private val sendHandler: SendModule.ISendHandler,
            private val feeModuleDelegate: IFeeModuleDelegate,
            private val customPriorityUnit: CustomPriorityUnit?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val feeRateProvider = FeeRateProviderFactory.provider(coin.coinType)
            val feeCoinData = App.feeCoinProvider.feeCoinData(coin.coinType)
            val feeCoin = feeCoinData?.first ?: coin

            val baseCurrency = App.currencyManager.baseCurrency
            val helper = SendFeePresenterHelper(App.numberFormatter, feeCoin, baseCurrency)

            val presenter = SendFeePresenter(
                helper,
                coin,
                feeCoinData,
                customPriorityUnit,
                FeeRateAdjustmentHelper(App.appConfigProvider),

                baseCurrency,
                App.marketKit,
                feeRateProvider,
                feeCoin
            )

            presenter.moduleDelegate = feeModuleDelegate
            sendHandler.feeModule = presenter

            return presenter as T
        }
    }

}

class FeeRateAdjustmentInfo(
        var amountInfo: SendAmountInfo,
        var xRate: BigDecimal?,
        val currency: Currency,
        var balance: BigDecimal?
)
