package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


object SendFeeModule {

    interface IView{
        fun onFeePriorityChange(feeRatePriority: FeeRatePriority)
        fun setPrimaryFee(feeAmount: String?)
        fun setSecondaryFee(feeAmount: String?)
        fun setInsufficientFeeBalanceError(coinCode: String, fee: BigDecimal)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onFeeSliderChange(progress: Int)
        fun onFeeUpdated(fee: BigDecimal?)
        fun onInputTypeUpdated(inputType: SendModule.InputType)
        fun getFeePriority(): FeeRatePriority
        fun onInsufficientFeeBalanceError(coinCode: String, fee: BigDecimal)
        fun getFeeCoinValue(): CoinValue
        fun getFeeCurrencyValue(): CurrencyValue?
    }

    interface IInteractor {
        fun getRate(coinCode: String, currencyCode: String)
    }

    interface IInteractorDelegate {
        fun onRateFetched(latestRate: Rate?)
    }

    fun init(view: SendFeeViewModel, coinCode: String) {
        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val feeCoinCode = adapter.feeCoinCode ?: coinCode
        val baseCurrency = App.currencyManager.baseCurrency
        val helper = SendFeePresenterHelper(App.numberFormatter, feeCoinCode, baseCurrency)
        val interactor = SendFeeInteractor(App.rateStorage)
        val presenter = SendFeePresenter(interactor, helper, feeCoinCode, baseCurrency)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
