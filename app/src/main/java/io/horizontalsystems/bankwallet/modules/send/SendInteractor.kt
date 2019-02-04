package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode

class SendInteractor(private val currencyManager: ICurrencyManager,
                     private val rateStorage: IRateStorage,
                     private val localStorage: ILocalStorage,
                     private val clipboardManager: IClipboardManager,
                     private val wallet: Wallet,
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

    override val coinTitle: String
        get() = wallet.title

    override val coinCode: CoinCode
        get() = wallet.coinCode

    override val addressFromClipboard: String?
        get() = clipboardManager.getCopiedText()

    private var rate: Rate? = null
    private val disposables = CompositeDisposable()

    override fun retrieveRate() {
        disposables.add(
                rateStorage.latestRateObservable(wallet.coinCode, currencyManager.baseCurrency.code)
                        .take(1)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            rate = if (it.expired) null else it
                            delegate?.didRateRetrieve()
                        }
        )
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return wallet.adapter.parsePaymentAddress(address)
    }

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: BigDecimal): BigDecimal? {
        val rate = this.rate ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount.times(rate.value)
            SendModule.InputType.CURRENCY -> amount.divide(rate.value, 8, RoundingMode.DOWN).stripTrailingZeros()
        }
    }

    override fun getTotalBalanceMinusFee(inputType: SendModule.InputType, address: String?): BigDecimal {
        return try{
            val fee = wallet.adapter.fee(wallet.adapter.balance, address, false)
            when(inputType){
                SendModule.InputType.COIN -> wallet.adapter.balance.minus(fee)
                else -> {
                    val safeRate = rate?.value ?: BigDecimal.ZERO
                    var feeInCurrency = fee.multiply(safeRate)
                    feeInCurrency = feeInCurrency.setScale(2, RoundingMode.CEILING)
                    val balanceInCurrency = wallet.adapter.balance.multiply(safeRate)
                    balanceInCurrency.subtract(feeInCurrency)
                }
            }
        } catch (e:  Error.InsufficientAmount) {
            wallet.adapter.balance
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput, senderPay: Boolean): SendModule.State {

        val coin = wallet.coinCode
        val adapter = wallet.adapter
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = rate?.value

        val decimal = if(input.inputType == SendModule.InputType.COIN) Math.min(adapter.decimal, appConfigProvider.maxDecimal) else appConfigProvider.fiatDecimal

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

        try {
            state.coinValue?.let { coinValue ->
                if ((state.coinValue?.value ?: BigDecimal.ZERO) > BigDecimal.ZERO) {
                    state.feeCoinValue = CoinValue(coin, adapter.fee(coinValue.value, input.address, senderPay))
                } else {
                    state.feeCoinValue = CoinValue(coin, BigDecimal.ZERO)
                }
            }
        } catch (e: Error.InsufficientAmount) {
            state.feeCoinValue = CoinValue(coin, e.fee)
            state.amountError = getAmountError(input.inputType, e.fee)
        }

        rateValue?.let {
            state.feeCoinValue?.let { feeCoinValue ->
                state.feeCurrencyValue = CurrencyValue(baseCurrency, feeCoinValue.value.times(rateValue))
            }
        }

        return state
    }

    private fun getAmountError(inputType: SendModule.InputType, fee: BigDecimal): SendModule.AmountError? {
        var balanceMinusFee = wallet.adapter.balance - fee
        if (balanceMinusFee < BigDecimal.ZERO) {
            balanceMinusFee = BigDecimal.ZERO
        }

        return when (inputType) {
            SendModule.InputType.COIN -> {
                SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(wallet.coinCode, balanceMinusFee)))
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

        var computedAmount: BigDecimal? = null

        if (userInput.inputType == SendModule.InputType.COIN) {
            computedAmount = userInput.amount
        } else {
            val rateValue = rate?.value
            if (rateValue != null) {
                computedAmount = userInput.amount / rateValue
            }
        }

        val amount = computedAmount

        if (amount == null) {
            delegate?.didFailToSend(SendError.NoAmount())
            return
        }

        wallet.adapter.send(address, amount) { error ->
            when (error) {
                null -> delegate?.didSend()
                else -> delegate?.didFailToSend(error)
            }
        }
    }
}
