package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.Error
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SendInteractor(private val currencyManager: ICurrencyManager,
                     private val rateStorage: IRateStorage,
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

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: Double): Double? {
        val rate = this.rate ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount * rate.value
            SendModule.InputType.CURRENCY -> amount / rate.value
        }
    }

    override fun getTotalBalanceMinusFee(inputType: SendModule.InputType, address: String?): Double {
        val fee = wallet.adapter.fee(wallet.adapter.balance, address, false)
        val balanceMinusFee = wallet.adapter.balance- fee
        return when(inputType){
            SendModule.InputType.COIN -> balanceMinusFee
            else -> balanceMinusFee * (rate?.value ?: 0.0)
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput, senderPay: Boolean): SendModule.State {

        val coin = wallet.coinCode
        val adapter = wallet.adapter
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = rate?.value

        val state = SendModule.State(input.inputType)

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
                    state.currencyValue = CurrencyValue(baseCurrency, input.amount * it)
                }
            }
            SendModule.InputType.CURRENCY -> {
                state.currencyValue = CurrencyValue(baseCurrency, input.amount)
                rateValue?.let {
                    state.coinValue = CoinValue(coin, input.amount / it)
                }
            }
        }

        try {
            state.coinValue?.let { coinValue ->
                state.feeCoinValue = CoinValue(coin, adapter.fee(coinValue.value, input.address, senderPay))
            }
        } catch (e: Error.InsufficientAmount) {
            state.feeCoinValue = CoinValue(coin, e.fee)
            state.amountError = getAmountError(input.inputType, e.fee)
        }

        rateValue?.let {
            state.feeCoinValue?.let { feeCoinValue ->
                state.feeCurrencyValue = CurrencyValue(baseCurrency, rateValue * feeCoinValue.value)
            }
        }

        return state
    }

    private fun getAmountError(inputType: SendModule.InputType, fee: Double): SendModule.AmountError? {
        var balanceMinusFee = wallet.adapter.balance - fee
        if (balanceMinusFee < 0) {
            balanceMinusFee = 0.0
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
