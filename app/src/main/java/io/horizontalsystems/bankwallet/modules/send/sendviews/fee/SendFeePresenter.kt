package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


class SendFeePresenter(
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val baseCoin: Coin,
        private val baseCurrency: Currency,
        private val feeCoinData: Pair<Coin, String>?)
    : SendFeeModule.IViewDelegate, SendFeeModule.IInteractorDelegate, SendFeeModule.IFeeModule {

    var view: SendFeeModule.IView? = null
    var moduleDelegate: SendFeeModule.IFeeModuleDelegate? = null

    private var rate: Rate? = null
    private var fee: BigDecimal = BigDecimal.ZERO
    private var inputType = SendModule.InputType.COIN
    private var feePriority: FeeRatePriority = FeeRatePriority.MEDIUM

    // SendFeeModule.IFeeModule

    override var feeRate = 0L
        private set

    override val coinValue: CoinValue
        get() = CoinValue(coin.code, fee)

    override val currencyValue: CurrencyValue?
        get() = rate?.let { CurrencyValue(baseCurrency, fee.multiply(it.value)) }

    override fun setFee(fee: BigDecimal) {
        this.fee = fee
        updateView()
    }

    override fun setInputType(inputType: SendModule.InputType) {
        this.inputType = inputType
        updateView()
    }

    // SendFeeModule.IViewDelegate

    private val coin: Coin
        get() = feeCoinData?.first ?: baseCoin

    override fun onViewDidLoad() {
        interactor.getRate(coin.code)
        feeRate = interactor.getFeeRate(feePriority)
    }

    override fun onRateFetched(latestRate: Rate?) {
        rate = latestRate
        updateView()
    }

    override fun onFeeSliderChange(progress: Int) {
        feePriority = FeeRatePriority.valueOf(progress)
        feeRate = interactor.getFeeRate(feePriority)

        moduleDelegate?.onFeePriorityChange(feePriority)
    }

    private fun updateView() {
        val reversedInputType = if (inputType == SendModule.InputType.COIN) SendModule.InputType.CURRENCY else SendModule.InputType.COIN

        view?.setPrimaryFee(helper.feeAmount(fee, inputType, rate))
        view?.setSecondaryFee(helper.feeAmount(fee, reversedInputType, rate))
    }

}
