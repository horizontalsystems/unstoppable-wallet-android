package io.horizontalsystems.bankwallet.modules.send.submodules.fee

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


object SendFeeModule {

    class InsufficientFeeBalance(val coin: Coin, val coinProtocol: String, val feeCoin: Coin, val fee: CoinValue) : Exception()

    interface IView {
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(insufficientFeeBalance: InsufficientFeeBalance?)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onFeeSliderChange(progress: Int)
        fun onClear()
    }

    interface IInteractor {
        fun getRate(coinCode: String)
        fun getFeeRate(feeRatePriority: FeeRatePriority): Long
        fun clear()
    }

    interface IInteractorDelegate {
        fun onRateFetched(latestRate: Rate?)
    }

    interface IFeeModule {
        val isValid: Boolean
        val feeRate: Long

        val coinValue: CoinValue
        val currencyValue: CurrencyValue?

        fun setFee(fee: BigDecimal)
        fun setAvailableFeeBalance(availableFeeBalance: BigDecimal)
        fun setInputType(inputType: SendModule.InputType)
    }

    interface IFeeModuleDelegate {
        fun onUpdateFeeRate(feeRate: Long)
    }

    fun init(view: SendFeeViewModel, coin: Coin, moduleDelegate: IFeeModuleDelegate?): IFeeModule {
        val feeRateProvider = FeeRateProviderFactory.provider(coin)
        val feeCoinData = App.feeCoinProvider.feeCoinData(coin)
        val feeCoinCode = (feeCoinData?.first ?: coin).code

        val baseCurrency = App.currencyManager.baseCurrency
        val helper = SendFeePresenterHelper(App.numberFormatter, feeCoinCode, baseCurrency)
        val interactor = SendFeeInteractor(App.rateStorage, feeRateProvider, App.currencyManager)
        val presenter = SendFeePresenter(interactor, helper, coin, baseCurrency, feeCoinData)

        view.delegate = presenter

        presenter.view = view
        presenter.moduleDelegate = moduleDelegate

        interactor.delegate = presenter

        return presenter
    }

}
