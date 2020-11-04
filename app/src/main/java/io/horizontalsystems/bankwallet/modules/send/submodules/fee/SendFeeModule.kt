package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.FeeRateInfo
import io.horizontalsystems.bankwallet.entities.FeeState
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import java.math.BigDecimal


object SendFeeModule {

    class InsufficientFeeBalance(val coin: Coin, val coinProtocol: String, val feeCoin: Coin, val fee: CoinValue) :
            Exception()

    interface IView {
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(insufficientFeeBalance: InsufficientFeeBalance?)
        fun setDuration(duration: Long?)
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
        fun onChangeFeeRate(feeRateInfo: FeeRateInfo)
        fun onChangeFeeRateValue(value: Long)
        fun onClickFeeRatePriority()
    }

    interface IInteractor {
        fun getRate(coinCode: String): BigDecimal?
        fun syncFeeRate()
        fun onClear()
    }

    interface IInteractorDelegate {
        fun didUpdate(feeRates: List<FeeRateInfo>)
        fun didReceiveError(error: Exception)
        fun didUpdateExchangeRate(rate: BigDecimal)
    }

    interface IFeeModule {
        val isValid: Boolean
        val feeRateState: FeeState
        val feeRate: Long
        val primaryAmountInfo: AmountInfo
        val secondaryAmountInfo: AmountInfo?
        val duration: Long?

        fun setLoading(loading: Boolean)
        fun setFee(fee: BigDecimal)
        fun setError(externalError: Exception?)
        fun setAvailableFeeBalance(availableFeeBalance: BigDecimal)
        fun setInputType(inputType: SendModule.InputType)
        fun fetchFeeRate()
    }

    interface IFeeModuleDelegate {
        fun onUpdateFeeRate()
    }

    data class FeeRateInfoViewItem(val feeRateInfo: FeeRateInfo, val selected: Boolean)


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

            val presenter = SendFeePresenter(view, interactor, helper, coin, baseCurrency, feeCoinData, customPriorityUnit)

            presenter.moduleDelegate = feeModuleDelegate
            interactor.delegate = presenter
            sendHandler.feeModule = presenter

            return presenter as T
        }
    }

}
