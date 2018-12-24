package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendInteractor(private val currencyManager: ICurrencyManager,
                     private val rateManager: RateManager,
                     private val clipboardManager: IClipboardManager,
                     private val wallet: Wallet) : SendModule.IInteractor {

    sealed class SendError : Exception() {
        class NoAddress : SendError()
        class NoAmount : SendError()
    }

    override val clipboardHasPrimaryClip: Boolean
        get() = clipboardManager.hasPrimaryClip

    var delegate: SendModule.IInteractorDelegate? = null

    override val coinCode: CoinCode
        get() = wallet.coinCode

    override val addressFromClipboard: String?
        get() = clipboardManager.getCopiedText()

    private var rate: Rate? = null
    private val disposables = CompositeDisposable()

    override fun retrieveRate() {
        disposables.add(
                rateManager.rate(wallet.coinCode, currencyManager.baseCurrency.code)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally {
                            delegate?.didRateRetrieve()
                        }
                        .subscribe {
                            rate = if (it.expired) null else it
                        }
        )
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return wallet.adapter.parsePaymentAddress(address)
    }

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: Double): Double? {
        val rate = this.rate ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount * rate.value
            SendModule.InputType.CURRENCY -> amount / rate.value
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput): SendModule.State {

        val coin = wallet.coinCode
        val adapter = wallet.adapter
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = rate?.value

        val state = SendModule.State(input.inputType)

        when (input.inputType) {
            SendModule.InputType.COIN -> {
                state.coinValue = CoinValue(coin, input.amount)
                rateValue?.let {
                    state.currencyValue = CurrencyValue(baseCurrency, input.amount * it)
                }

                val balance = adapter.balance
                if (balance < input.amount) {
                    state.amountError = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(coin, balance)))
                }

            }
            SendModule.InputType.CURRENCY -> {
                rateValue?.let {
                    state.coinValue = CoinValue(coin, input.amount / it)
                }
                state.currencyValue = CurrencyValue(baseCurrency, input.amount)

                if (rateValue != null) {
                    val currencyBalance = adapter.balance * rateValue
                    if (currencyBalance < input.amount) {
                        state.amountError = SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(baseCurrency, currencyBalance)))
                    }
                }

            }
        }

        state.address = input.address
        val address = input.address

        if (address != null) {
            try {
                adapter.validate(address)
            } catch (e: Exception) {
                state.addressError = SendModule.AddressError.InvalidAddress()
            }
        }

        try {
            state.coinValue?.let { coinValue ->
                state.feeCoinValue = CoinValue(coin, adapter.fee(coinValue.value, input.address, true))
            }
        } catch (e: Exception) {
        }

        rateValue?.let {
            state.feeCoinValue?.let { feeCoinValue ->
                state.feeCurrencyValue = CurrencyValue(baseCurrency, rateValue * feeCoinValue.value)
            }
        }

        return state
    }

    override fun send(userInput: SendModule.UserInput) {
        val address = userInput.address
        if (address == null) {
            delegate?.didFailToSend(SendError.NoAddress())
            return
        }

        var computedAmount: Double? = null

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
