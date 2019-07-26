package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import java.math.RoundingMode


class SendAmountPresenter(private val interactor: SendAmountModule.IInteractor, private val presenterHelper: SendAmountPresenterHelper)
    : SendAmountModule.IViewDelegate, SendAmountModule.IInteractorDelegate {

    var view: SendAmountViewModel? = null

    private var coinAmount: BigDecimal? = null
    private var rate: Rate? = null

    override fun onViewDidLoad() {
        interactor.retrieveRate()
        view?.addTextChangeListener()
        updateAmount()
    }

    override fun getCoinAmount(): BigDecimal? {
        return coinAmount
    }

    override fun onMaxClick() {
        view?.getAvailableBalance()
    }

    override fun onSwitchClick() {
        view?.removeTextChangeListener()
        val newInputType = when (interactor.defaultInputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }
        interactor.defaultInputType = newInputType
        updateAmount()
        view?.addTextChangeListener()
    }

    override fun onAmountChange(amountString: String) {
        updateMaxButtonVisibility(amountString.isEmpty())

        val amount = parseInput(amountString)
        val decimal = presenterHelper.decimal(interactor.defaultInputType)
        if(amount.scale() > decimal) {
            onNumberScaleExceeded(amount, decimal)
        } else {
            coinAmount = presenterHelper.getCoinAmount(amount, interactor.defaultInputType, rate)
            view?.setHintInfo(presenterHelper.getHintInfo(coinAmount, interactor.defaultInputType, rate))
            view?.notifyMainViewModelOnAmountChange(coinAmount)
        }
    }

    override fun didRateRetrieve(rate: Rate?) {
        this.rate = rate
        updateAmount()
    }

    override fun didFeeRateRetrieve() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAvailableBalanceRetreived(availableBalance: BigDecimal) {
        coinAmount = availableBalance
        updateAmount()
    }

    private fun onNumberScaleExceeded(amount: BigDecimal, decimal: Int) {
        val amountNumber = amount.setScale(decimal, RoundingMode.FLOOR)
        val revertedInput = amountNumber.toPlainString()
        view?.revertInput(revertedInput)
    }

    private fun updateMaxButtonVisibility(empty: Boolean) {
        view?.setMaxButtonVisible(empty)
    }

    private fun parseInput(amountString: String): BigDecimal {
        return when {
            amountString != "" -> amountString.toBigDecimalOrNull() ?: BigDecimal.ZERO
            else -> { BigDecimal.ZERO }
        }
    }

    private fun updateAmount() {
        val amountInfo = presenterHelper.getAmountInfo(coinAmount, interactor.defaultInputType, rate)
        val hintInfo = presenterHelper.getHintInfo(coinAmount, interactor.defaultInputType, rate)
        val prefix = presenterHelper.getAmountPrefix(interactor.defaultInputType, rate)

        view?.setAmountPrefix(prefix)
        view?.setAmountInfo(amountInfo)
        view?.setHintInfo(hintInfo)
    }

}
