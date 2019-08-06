package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

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
        fun setInsufficientFeeBalanceError(feeCoinValue: CoinValue)
    }

    interface IViewDelegate {
        val coinCode: String
        val baseCoinName: String
        val tokenProtocol: String
        val validState: Boolean

        fun onViewDidLoad()
        fun onFeeSliderChange(progress: Int)
        fun onFeeUpdated(fee: BigDecimal?)
        fun onInputTypeUpdated(inputType: SendModule.InputType)
        fun getFeeRate(): Long
        fun onInsufficientFeeBalanceError(fee: BigDecimal)
        fun getFeeCoinValue(): CoinValue
        fun getFeeCurrencyValue(): CurrencyValue?
    }

    interface IInteractor {
        fun getRate(coinCode: String, currencyCode: String)
        fun getFeeRate(feeRatePriority: FeeRatePriority): Long
    }

    interface IInteractorDelegate {
        fun onRateFetched(latestRate: Rate?)
    }

    fun init(view: SendFeeViewModel, coinCode: String) {
        TODO()
//        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
//        val feeCoinCode = adapter.feeCoinCode ?: coinCode
//        val baseCurrency = App.currencyManager.baseCurrency
//        val baseCoinName = getBaseCoinName(adapter)
//        val tokenProtocol = getTokenProtocol(adapter)
//        val helper = SendFeePresenterHelper(App.numberFormatter, feeCoinCode, baseCurrency)
//        val interactor = SendFeeInteractor(App.rateStorage, adapter)
//        val presenter = SendFeePresenter(interactor, helper, coinCode, feeCoinCode, baseCurrency, baseCoinName, tokenProtocol)
//
//        view.delegate = presenter
//        presenter.view = view
//        interactor.delegate = presenter
    }

//    private fun getBaseCoinName(adapter: IAdapter): String {
//        return when(adapter) {
//            is BinanceAdapter -> "Binance"
//            is Erc20Adapter -> "Ethereum"
//            else -> adapter.wallet.coin.title
//        }
//    }

//    private fun getTokenProtocol(adapter: IAdapter): String {
//        return when(adapter) {
//            is BinanceAdapter -> "BEP2"
//            is Erc20Adapter -> "ERC20"
//            else -> ""
//        }
//    }

}
