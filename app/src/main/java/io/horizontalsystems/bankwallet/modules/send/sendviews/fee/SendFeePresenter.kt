package io.horizontalsystems.bankwallet.modules.send.sendviews.fee

import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal


class SendFeePresenter(
        private val interactor: SendFeeModule.IInteractor,
        private val helper: SendFeePresenterHelper,
        private val feeCoinCode: String,
        private val currencyCode: String)
    : SendFeeModule.IViewDelegate, SendFeeModule.IInteractorDelegate {

    var view: SendFeeViewModel? = null
    private var rate: Rate? = null
    private var fee: BigDecimal = BigDecimal.ZERO
    private var inputType = SendModule.InputType.COIN
    private var feePriority: FeeRatePriority = FeeRatePriority.MEDIUM

    override fun onViewDidLoad() {
        interactor.getRate(feeCoinCode, currencyCode)
    }

    override fun onRateFetched(latestRate: Rate?) {
        rate = latestRate
        updateView()
    }

    override fun onFeeSliderChange(progress: Int) {
        feePriority = FeeRatePriority.valueOf(progress)
        view?.onFeePriorityChange(feePriority)
    }

    override fun onInputTypeUpdated(inputType: SendModule.InputType) {
        this.inputType = inputType
        updateView()
    }

    override fun getFeePriority(): FeeRatePriority {
        return feePriority
    }

    override fun onFeeUpdated(fee: BigDecimal?) {
        this.fee = fee ?: BigDecimal.ZERO
        updateView()
    }

    override fun onInsufficientFeeBalanceError(coinCode: String, fee: BigDecimal) {
        view?.setInsufficientFeeBalanceError(coinCode, fee)
    }

    private fun updateView(){
        val reversedInputType = if (inputType == SendModule.InputType.COIN) SendModule.InputType.CURRENCY else SendModule.InputType.COIN

        view?.setPrimaryFee(helper.feeAmount(fee, inputType, rate))
        view?.setSecondaryFee(helper.feeAmount(fee, reversedInputType, rate))
    }
}
