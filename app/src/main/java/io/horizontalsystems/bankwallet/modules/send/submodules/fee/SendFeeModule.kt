package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.FeeRateState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.math.BigInteger


object SendFeeModule {

    class InsufficientFeeBalance(val coin: Coin, val coinProtocol: String, val feeCoin: Coin, val fee: CoinValue) :
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
        fun getRate(coinType: CoinType): BigDecimal?
        fun syncFeeRate(feeRatePriority: FeeRatePriority)
        fun onClear()
    }

    interface IInteractorDelegate {
        fun didUpdate(feeRate: BigInteger)
        fun didReceiveError(error: Exception)
        fun didUpdateExchangeRate(rate: BigDecimal)
    }

    interface IFeeModule {
        val isValid: Boolean
        val feeRateState: FeeRateState
        val feeRate: Long?
        val primaryAmountInfo: AmountInfo
        val secondaryAmountInfo: AmountInfo?

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
            private val coin: Coin,
            private val sendHandler: SendModule.ISendHandler,
            private val feeModuleDelegate: IFeeModuleDelegate,
            private val customPriorityUnit: CustomPriorityUnit?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val view = SendFeeView()
            val feeRateProvider = FeeRateProviderFactory.provider(coin)
            val feeCoinData = App.feeCoinProvider.feeCoinData(coin)
            val feeCoin = feeCoinData?.first ?: coin

            val baseCurrency = App.currencyManager.baseCurrency
            val helper = SendFeePresenterHelper(App.numberFormatter, feeCoin, baseCurrency)
            val interactor = SendFeeInteractor(baseCurrency, App.xRateManager, feeRateProvider, feeCoin)

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
