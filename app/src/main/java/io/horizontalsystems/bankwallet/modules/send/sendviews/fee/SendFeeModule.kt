package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

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

    interface IView {
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(feeCoinValue: CoinValue)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onFeeSliderChange(progress: Int)
    }

    interface IInteractor {
        fun getRate(coinCode: String)
        fun getFeeRate(feeRatePriority: FeeRatePriority): Long
    }

    interface IInteractorDelegate {
        fun onRateFetched(latestRate: Rate?)
    }

    interface IFeeModule {
        val feeRate: Long

        val coinValue: CoinValue
        val currencyValue: CurrencyValue?

        fun setFee(fee: BigDecimal)
        fun setInputType(inputType: SendModule.InputType)
    }

    interface IFeeModuleDelegate {
        fun onFeePriorityChange(feeRatePriority: FeeRatePriority)
    }

    fun init(view: SendFeeViewModel, coin: Coin, moduleDelegate: IFeeModuleDelegate?): IFeeModule {
        val feeRateProvider = FeeRateProviderFactory.provider(coin)
                ?: throw Exception("No FeeRateProvider")
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
