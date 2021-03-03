package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CoinValueInfo
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo.CurrencyValueInfo
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule.ValidationError.InsufficientBalance
import io.horizontalsystems.bankwallet.modules.send.submodules.amount.SendAmountModule.ValidationError.TooFewAmount
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.math.RoundingMode

class SendAmountPresenter(
        val view: SendAmountModule.IView,
        private val interactor: SendAmountModule.IInteractor,
        private val presenterHelper: SendAmountPresenterHelper,
        private val coin: Coin,
        private val baseCurrency: Currency,
        private val stringProvider: StringProvider)
    : ViewModel(), SendAmountModule.IViewDelegate, SendAmountModule.IInteractorDelegate, SendAmountModule.IAmountModule {

    var moduleDelegate: SendAmountModule.IAmountModuleDelegate? = null

    private var amount: BigDecimal? = null
    private var availableBalance: BigDecimal? = null
    private var minimumAmount: BigDecimal? = null
    private var maximumAmount: BigDecimal? = null
    private var minimumRequiredBalance: BigDecimal = BigDecimal.ZERO

    override var xRate: BigDecimal? = null
    override var sendAmountInfo: SendAmountInfo = SendAmountInfo.NotEntered

    override var inputType = SendModule.InputType.COIN
        private set

    override val coinAmount: CoinValue
        get() = CoinValue(coin, amount ?: BigDecimal.ZERO)

    override val fiatAmount: CurrencyValue?
        get() {
            val currencyAmount = xRate?.let { amount?.times(it) }
            return currencyAmount?.let { CurrencyValue(baseCurrency, it) }
        }

    override val currentAmount: BigDecimal
        get() = amount ?: BigDecimal.ZERO


    @Throws
    override fun primaryAmountInfo(): SendModule.AmountInfo {
        return when (inputType) {
            SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, validAmount()))
            SendModule.InputType.CURRENCY -> {
                this.xRate?.let { xRate ->
                    CurrencyValueInfo(CurrencyValue(baseCurrency, validAmount() * xRate))
                } ?: throw Exception("Invalid state")
            }
        }
    }

    override fun secondaryAmountInfo(): SendModule.AmountInfo? {
        return when (inputType.reversed()) {
            SendModule.InputType.COIN -> CoinValueInfo(CoinValue(coin, validAmount()))
            SendModule.InputType.CURRENCY -> {
                this.xRate?.let { xRate ->
                    CurrencyValueInfo(CurrencyValue(baseCurrency, validAmount() * xRate))
                }
            }
        }
    }

    @Throws
    override fun validAmount(): BigDecimal {
        val amount = this.amount ?: BigDecimal.ZERO

        if (amount <= BigDecimal.ZERO) {
            throw SendAmountModule.ValidationError.EmptyValue("amount")
        }

        validate()

        return amount
    }

    override fun setLoading(loading: Boolean) {
        view.setLoading(loading)
    }

    override fun setAmount(amount: BigDecimal) {
        this.amount = amount
        sendAmountInfo = SendAmountInfo.Entered(amount)

        syncAmount()
        syncHint()
        syncError()

        moduleDelegate?.onChangeAmount()
    }

    override fun setAvailableBalance(availableBalance: BigDecimal) {
        this.availableBalance = availableBalance

        syncMaxButton()
        syncAvailableBalance()
        syncError()
    }

    override fun setMinimumAmount(minimumAmount: BigDecimal) {
        this.minimumAmount = minimumAmount
    }

    override fun setMaximumAmount(maximumAmount: BigDecimal?) {
        this.maximumAmount = maximumAmount
    }

    override fun setMinimumRequiredBalance(minimumRequiredBalance: BigDecimal) {
        this.minimumRequiredBalance = minimumRequiredBalance
    }

    // SendModule.IViewDelegate

    override fun onViewDidLoad() {
        xRate = interactor.getRate()

        inputType = when {
            xRate == null -> SendModule.InputType.COIN
            else -> interactor.defaultInputType
        }

        moduleDelegate?.onChangeInputType(inputType)
        moduleDelegate?.onRateUpdated(xRate)

        syncAmountType()
        syncAmount()
        syncHint()
    }

    override fun onSwitchClick() {
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
        syncAvailableBalance()
    }

    override fun onAmountChange(amountString: String) {
        val amount = amountString.toBigDecimalOrNull()
        val decimal = presenterHelper.decimal(inputType)

        if (amount != null && amount.scale() > decimal) {
            val amountNumber = amount.setScale(decimal, RoundingMode.FLOOR)
            val revertedInput = amountNumber.toPlainString()
            view.revertAmount(revertedInput)
        } else {
            this.amount = presenterHelper.getCoinAmount(amount, inputType, xRate)

            sendAmountInfo = this.amount?.let { SendAmountInfo.Entered(it) } ?: SendAmountInfo.NotEntered

            syncHint()
            syncError()

            moduleDelegate?.onChangeAmount()
        }
    }

    override fun onMaxClick() {
        amount = availableBalance?.subtract(minimumRequiredBalance)
        sendAmountInfo = SendAmountInfo.Max

        syncAmount()
        syncHint()
        syncError()

        moduleDelegate?.onChangeAmount()
    }

    // IInteractorDelegate

    override fun didUpdateRate(rate: BigDecimal) {
        syncXRate(rate)
    }

    override fun willEnterForeground() {
        syncXRate(interactor.getRate())
    }

    // ViewModel

    override fun onCleared() {
        super.onCleared()
        interactor.onCleared()
    }

    // Internal methods

    private fun syncXRate(rate: BigDecimal?) {
        if (rate == xRate) {
            return
        }

        xRate = rate
        inputType = when (xRate) {
            null -> SendModule.InputType.COIN
            else -> interactor.defaultInputType
        }

        moduleDelegate?.onRateUpdated(rate)

        syncAmount()
        syncAvailableBalance()
        syncAmountType()
        syncHint()
    }

    private fun syncAmount() {
        val amount = presenterHelper.getAmount(amount, inputType, xRate)
        view.setAmount(amount)
    }

    private fun syncAvailableBalance() {
        presenterHelper.getAvailableBalance(availableBalance, inputType, xRate)?.let {
            view.setAvailableBalance(it)
        }
    }

    private fun syncAmountType() {
        val prefix = presenterHelper.getAmountPrefix(inputType, xRate) ?: ""
        view.setAmountType(prefix)
    }

    private fun syncHint() {
        var hint = presenterHelper.getHint(this.amount, inputType, xRate)
        view.setHintStateEnabled(hint != null)
        hint = hint ?: stringProvider.string(R.string.NotAvailable)
        view.setHint(hint)
    }

    private fun syncMaxButton() {
        val noneNullAvailableBalance = availableBalance ?: run {
            view.setMaxButtonVisible(false)
            return
        }

        amount?.let {
            if (it > BigDecimal.ZERO) {
                view.setMaxButtonVisible(false)
                return
            }
        }

        val hasSpendableBalance = noneNullAvailableBalance - minimumRequiredBalance > BigDecimal.ZERO
        view.setMaxButtonVisible(hasSpendableBalance)
    }

    private fun validate() {
        val amount = this.amount ?: return
        if (amount <= BigDecimal.ZERO) return

        minimumAmount?.let {
            if (amount < it) throw TooFewAmount(amountInfo(it))
        }

        availableBalance?.let {
            if (amount > it) throw InsufficientBalance(amountInfo(it))

            if (it - amount < minimumRequiredBalance)
                throw SendAmountModule.ValidationError.NotEnoughForMinimumRequiredBalance(CoinValue(coin, minimumRequiredBalance))
        }

        maximumAmount?.let {
            if (amount > it) throw SendAmountModule.ValidationError.MaxAmountLimit(amountInfo(it))
        }
    }

    private fun amountInfo(coinValue: BigDecimal): SendModule.AmountInfo? {
        return when (inputType) {
            SendModule.InputType.COIN -> {
                CoinValueInfo(CoinValue(coin, coinValue))
            }
            SendModule.InputType.CURRENCY -> {
                xRate?.let { rate ->
                    val value = coinValue.times(rate)
                    CurrencyValueInfo(CurrencyValue(baseCurrency, value))
                }
            }
        }
    }

    private fun syncError() {
        try {
            validate()
            view.setValidationError(null)

        } catch (e: SendAmountModule.ValidationError) {
            view.setValidationError(e)
        }
    }

}
