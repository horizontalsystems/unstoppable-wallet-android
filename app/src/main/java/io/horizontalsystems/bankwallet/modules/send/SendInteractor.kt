package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode

class SendInteractor(private val currencyManager: ICurrencyManager,
                     private val rateStorage: IRateStorage,
                     private val localStorage: ILocalStorage,
                     private val clipboardManager: IClipboardManager,
                     private val adapter: IAdapter,
                     private val appConfigProvider: IAppConfigProvider) : SendModule.IInteractor {

    sealed class SendError : Exception() {
        class NoAddress : SendError()
        class NoAmount : SendError()
    }

    override var defaultInputType: SendModule.InputType
        get() = localStorage.sendInputType ?: SendModule.InputType.COIN
        set(value) {
            localStorage.sendInputType = value
        }

    override val clipboardHasPrimaryClip: Boolean
        get() = clipboardManager.hasPrimaryClip

    var delegate: SendModule.IInteractorDelegate? = null

    override val coin: Coin
        get() = adapter.coin

    override val addressFromClipboard: String?
        get() = clipboardManager.getCopiedText()

    private var rate: Rate? = null
    private val disposables = CompositeDisposable()

    override fun retrieveRate() {
        disposables.add(
                rateStorage.latestRateObservable(adapter.coin.code, currencyManager.baseCurrency.code)
                        .take(1)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            rate = if (it.expired) null else it
                            if (rate != null) {
                                delegate?.didRateRetrieve()
                            }
                        }
        )
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return adapter.parsePaymentAddress(address)
    }

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: BigDecimal): BigDecimal? {
        val rate = this.rate ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount.times(rate.value)
            SendModule.InputType.CURRENCY -> amount.divide(rate.value, 8, RoundingMode.DOWN)
        }
    }

    override fun getTotalBalanceMinusFee(inputType: SendModule.InputType, address: String?): BigDecimal {
        val availableBalance = adapter.availableBalance(address)
        return when (inputType) {
            SendModule.InputType.COIN -> availableBalance
            else -> availableBalance.multiply(rate?.value ?: BigDecimal.ZERO)
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput): SendModule.State {

        val coin = adapter.coin.code
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = rate?.value

        val decimal = if (input.inputType == SendModule.InputType.COIN) Math.min(adapter.decimal, appConfigProvider.maxDecimal) else appConfigProvider.fiatDecimal

        val state = SendModule.State(decimal, input.inputType)

        state.address = input.address
        val address = input.address

        if (address != null) {
            try {
                adapter.validate(address)
            } catch (e: Exception) {
                state.addressError = SendModule.AddressError.InvalidAddress()
            }
        }

        when (input.inputType) {
            SendModule.InputType.COIN -> {
                state.coinValue = CoinValue(coin, input.amount)
                rateValue?.let {
                    state.currencyValue = CurrencyValue(baseCurrency, input.amount.times(it))
                }
            }
            SendModule.InputType.CURRENCY -> {
                state.currencyValue = CurrencyValue(baseCurrency, input.amount)
                rateValue?.let {
                    state.coinValue = CoinValue(coin, input.amount.divide(it, 8, RoundingMode.HALF_EVEN))
                }
            }
        }

        val errors = adapter.validate(state.coinValue?.value ?: BigDecimal.ZERO, address)

        for (error in errors) {
            when (error) {
                SendStateError.InsufficientAmount -> state.amountError = getAmountError(input)
                SendStateError.InsufficientFeeBalance -> state.feeError = getFeeError(input)
            }
        }

        state.coinValue?.let { coinValue ->
            if ((state.coinValue?.value ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                state.feeCoinValue = CoinValue(coin, adapter.fee(coinValue.value, input.address))
            } else {
                state.feeCoinValue = CoinValue(coin, BigDecimal.ZERO)
            }
        }

        rateValue?.let {
            state.feeCoinValue?.let { feeCoinValue ->
                state.feeCurrencyValue = CurrencyValue(baseCurrency, feeCoinValue.value.times(rateValue))
            }
        }

        return state
    }

    private fun getFeeError(input: SendModule.UserInput): SendModule.AmountError.Erc20FeeError? {
        adapter.feeCoinCode?.let {
            val fee = adapter.fee(input.amount, input.address)
            val coinValue = CoinValue(it, fee)
            return SendModule.AmountError.Erc20FeeError(adapter.coin.code, coinValue)
        } ?: return null
    }

    private fun getAmountError(input: SendModule.UserInput): SendModule.AmountError? {
        var balanceMinusFee = adapter.availableBalance(input.address)
        if (balanceMinusFee < BigDecimal.ZERO) {
            balanceMinusFee = BigDecimal.ZERO
        }

        return when (input.inputType) {
            SendModule.InputType.COIN -> {
                SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(adapter.coin.code, balanceMinusFee)))
            }
            SendModule.InputType.CURRENCY -> {
                rate?.value?.let {
                    val currencyBalanceMinusFee = balanceMinusFee * it
                    SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currencyManager.baseCurrency, currencyBalanceMinusFee)))
                }
            }
        }
    }

    override fun send(userInput: SendModule.UserInput) {
        val address = userInput.address
        if (address == null) {
            delegate?.didFailToSend(SendError.NoAddress())
            return
        }

        val computedAmount = when (userInput.inputType) {
            SendModule.InputType.COIN -> userInput.amount
            SendModule.InputType.CURRENCY -> convertedAmountForInputType(SendModule.InputType.CURRENCY, userInput.amount)
        }

        if (computedAmount == null || computedAmount.compareTo(BigDecimal.ZERO) == 0) {
            delegate?.didFailToSend(SendError.NoAmount())
            return
        }

        adapter.send(address, computedAmount) { error ->
            when (error) {
                null -> delegate?.didSend()
                else -> delegate?.didFailToSend(error)
            }
        }
    }

    override fun clear() {
        disposables.clear()
    }

}
