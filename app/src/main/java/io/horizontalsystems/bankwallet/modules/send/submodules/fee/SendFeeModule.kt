package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.math.BigInteger


object SendFeeModule {

    class InsufficientFeeBalance(val coin: PlatformCoin, val coinProtocol: String, val feeCoin: PlatformCoin, val fee: CoinValue) :
            Exception()

    interface IView {
        fun setAdjustableFeeVisible(visible: Boolean)
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(insufficientFeeBalance: InsufficientFeeBalance?)
        fun setFeePriority(priority: FeeRatePriority)
        fun showFeeRatePrioritySelector(feeRates: List<FeeRateInfoViewItem>)
        fun showCustomFeePriority(show: Boolean)
        fun setCustomFeeParams(value: Int, range: IntRange, label: String?)

        fun setLoading(loading: Boolean)
        fun setFee(fee: AmountInfo, convertedFee: AmountInfo?)
        fun setError(error: Exception?)
        fun showLowFeeWarning(show: Boolean)

    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onChangeFeeRate(feeRatePriority: FeeRatePriority)
        fun onChangeFeeRateValue(value: Int)
        fun onClickFeeRatePriority()
    }

    interface IInteractor {
        val feeRatePriorityList: List<FeeRatePriority>
        val defaultFeeRatePriority: FeeRatePriority?
        fun getRate(coinUid: String): BigDecimal?
        fun syncFeeRate(feeRatePriority: FeeRatePriority)
        fun onClear()
    }

    interface IInteractorDelegate {
        fun didUpdate(feeRate: BigInteger, feeRatePriority: FeeRatePriority)
        fun didReceiveError(error: Exception)
        fun didUpdateExchangeRate(rate: BigDecimal)
    }

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
        fun setInputType(inputType: SendModule.InputType)
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

            val view = SendFeeView()
            val feeRateProvider = FeeRateProviderFactory.provider(coin.coinType)
            val feeCoinData = App.feeCoinProvider.feeCoinData(coin.coinType)
            val feeCoin = feeCoinData?.first ?: coin

            val baseCurrency = App.currencyManager.baseCurrency
            val helper = SendFeePresenterHelper(App.numberFormatter, feeCoin, baseCurrency)
            val interactor = SendFeeInteractor(baseCurrency, App.marketKit, feeRateProvider, feeCoin)

            val presenter = SendFeePresenter(view, interactor, helper, coin, baseCurrency, feeCoinData, customPriorityUnit, FeeRateAdjustmentHelper(App.appConfigProvider))

            presenter.moduleDelegate = feeModuleDelegate
            interactor.delegate = presenter
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
