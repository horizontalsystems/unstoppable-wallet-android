package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import java.math.RoundingMode


class SendAmountPresenter(
        private val interactor: SendAmountModule.IInteractor,
        private val presenterHelper: SendAmountPresenterHelper,
        private val coinCode: String,
        private val baseCurrency: Currency)
    : SendAmountModule.IViewDelegate, SendAmountModule.IInteractorDelegate, SendAmountModule.IAmountModule {

    var view: SendAmountModule.IView? = null
    var moduleDelegate: SendAmountModule.IAmountModuleDelegate? = null

    private var amount: BigDecimal? = null
    private var availableBalance: BigDecimal? = null
    private var rate: Rate? = null

    // SendAmountModule.IAmountModule

    override var inputType = SendModule.InputType.COIN
        private set

    override val coinAmount: CoinValue
        get() = CoinValue(coinCode, amount ?: BigDecimal.ZERO)

    override val fiatAmount: CurrencyValue?
        get() {
            val currencyAmount = rate?.let { amount?.times(it.value) }
            return currencyAmount?.let { CurrencyValue(baseCurrency, it) }
        }

    override val validAmount: BigDecimal?
        get() {
            val amount = this.amount ?: return null
            val availableBalance = this.availableBalance ?: return null

            if (availableBalance < amount) {
                return null
            }

            return amount
        }

    override fun setAmount(amount: BigDecimal) {
        this.amount = amount

        syncAmount()
        syncHint()
        syncMaxButton()
        syncError()

        moduleDelegate?.onChangeAmount()
    }

    override fun setAvailableBalance(availableBalance: BigDecimal) {
        this.availableBalance = availableBalance

        syncError()
    }

    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        interactor.retrieveRate()
        view?.addTextChangeListener()

        syncAmountType()
        syncSwitchButton()
        syncHint()
    }

    override fun onSwitchClick() {
        view?.removeTextChangeListener()

        inputType = when (inputType) {
            SendModule.InputType.CURRENCY -> SendModule.InputType.COIN
            else -> SendModule.InputType.CURRENCY
        }
        interactor.defaultInputType = inputType
        moduleDelegate?.onChangeInputType(inputType)

        syncAmountType()
        syncAmount()
        syncHint()
        syncError()

        view?.addTextChangeListener()
    }

    override fun onAmountChange(amountString: String) {
        val amount = amountString.toBigDecimalOrNull()
        val decimal = presenterHelper.decimal(inputType)

        if (amount != null && amount.scale() > decimal) {
            val amountNumber = amount.setScale(decimal, RoundingMode.FLOOR)
            val revertedInput = amountNumber.toPlainString()
            view?.revertAmount(revertedInput)
        } else {
            this.amount = presenterHelper.getCoinAmount(amount, inputType, rate)

            syncHint()
            syncMaxButton()
            syncError()

            moduleDelegate?.onChangeAmount()
        }
    }

    override fun onMaxClick() {
        amount = availableBalance

        syncAmount()
        syncHint()
        syncMaxButton()
        syncError()

        moduleDelegate?.onChangeAmount()
    }

    override fun didRateRetrieve(rate: Rate?) {
        this.rate = rate

        rate?.let {
            inputType = interactor.defaultInputType

            moduleDelegate?.onChangeInputType(inputType)
            syncSwitchButton()
        }

        syncAmountType()
        syncAmount()
        syncHint()
    }

    private fun syncAmount() {
        val amount = presenterHelper.getAmount(amount, inputType, rate)
        view?.setAmount(amount)
    }

    private fun syncAmountType() {
        val prefix = presenterHelper.getAmountPrefix(inputType, rate)
        view?.setAmountType(prefix)
    }

    private fun syncHint() {
        val hint = presenterHelper.getHint(this.amount, inputType, rate)
        view?.setHint(hint)
    }

    private fun syncMaxButton() {
        val visible = amount?.let { it == BigDecimal.ZERO } ?: true
        view?.setMaxButtonVisible(visible)
    }

    private fun syncSwitchButton() {
        view?.setSwitchButtonEnabled(rate != null)
    }

    private fun syncError() {
        val amount = this.amount ?: return
        val availableBalance = this.availableBalance ?: return

        if (availableBalance < amount) {
            view?.setHintErrorBalance(presenterHelper.getBalanceForHintError(inputType, availableBalance, rate))
        } else {
            view?.setHintErrorBalance(null)
        }
    }

}
