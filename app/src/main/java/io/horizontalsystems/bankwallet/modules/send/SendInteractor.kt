package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit

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
        get() = adapter.wallet.coin

    override val addressFromClipboard: String?
        get() = clipboardManager.getCopiedText()

    private var exchangeRate: Rate? = null
    private var exchangeFeeRate: Rate? = null
    private val disposables = CompositeDisposable()

    override fun retrieveRate() {
        disposables.clear()

        disposables.add(
                rateStorage.latestRateObservable(adapter.wallet.coin.code, currencyManager.baseCurrency.code)
                        .take(1)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            exchangeRate = if (it.expired) null else it
                            delegate?.didRateRetrieve(exchangeRate)
                        }
        )

        adapter.feeCoinCode?.let {
            disposables.add(
                    rateStorage.latestRateObservable(it, currencyManager.baseCurrency.code)
                            .take(1)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe { fetchedRate ->
                                exchangeFeeRate = if (fetchedRate.expired) null else fetchedRate
                                if (exchangeFeeRate != null) {
                                    delegate?.didFeeRateRetrieve()
                                }
                            }
            )
        }

        disposables.add(
                Flowable.interval(1, TimeUnit.MINUTES)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            if (exchangeRate?.expired == true) {
                                exchangeRate = null
                                delegate?.didRateRetrieve(exchangeRate)
                            }
                        }
        )
    }

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return adapter.parsePaymentAddress(address)
    }

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: BigDecimal): BigDecimal? {
        val rate = this.exchangeRate ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount.times(rate.value)
            SendModule.InputType.CURRENCY -> amount.divide(rate.value, 8, RoundingMode.DOWN)
        }
    }

    override fun getTotalBalanceMinusFee(inputType: SendModule.InputType, address: String?, feeRate: FeeRatePriority): BigDecimal {
        val availableBalance = adapter.availableBalance(address, feeRate)
        return when (inputType) {
            SendModule.InputType.COIN -> availableBalance
            else -> availableBalance.multiply(exchangeRate?.value ?: BigDecimal.ZERO)
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput): SendModule.State {

        val coin = adapter.wallet.coin.code
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = exchangeRate?.value

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

        val errors = adapter.validate(state.coinValue?.value
                ?: BigDecimal.ZERO, address, input.feePriority)

        for (error in errors) {
            when (error) {
                SendStateError.InsufficientAmount -> state.amountError = getAmountError(input, input.feePriority)
                SendStateError.InsufficientFeeBalance -> state.feeError = getFeeError(input, input.feePriority)
            }
        }

        state.coinValue?.let { coinValue ->
            val coinCode = adapter.feeCoinCode ?: adapter.wallet.coin.code
            if (coinValue.value > BigDecimal.ZERO) {
                state.feeCoinValue = CoinValue(coinCode, adapter.fee(coinValue.value, input.address, input.feePriority))
            } else {
                state.feeCoinValue = CoinValue(coinCode, BigDecimal.ZERO)
            }
        }

        var feeCurrencyRate: BigDecimal? = null
        adapter.feeCoinCode?.let {
            feeCurrencyRate = exchangeFeeRate?.value
        } ?: run {
            feeCurrencyRate = rateValue
        }

        feeCurrencyRate?.let { feeCurRate ->
            state.feeCoinValue?.let { feeCoinValue ->
                state.feeCurrencyValue = CurrencyValue(baseCurrency, feeCoinValue.value.times(feeCurRate))
            }
        }

        return state
    }

    private fun getFeeError(input: SendModule.UserInput, feePriority: FeeRatePriority): SendModule.AmountError.Erc20FeeError? {
        adapter.feeCoinCode?.let {
            val fee = adapter.fee(input.amount, input.address, feePriority)
            val coinValue = CoinValue(it, fee)
            return SendModule.AmountError.Erc20FeeError(adapter.wallet.coin.code, coinValue)
        } ?: return null
    }

    private fun getAmountError(input: SendModule.UserInput, feePriority: FeeRatePriority): SendModule.AmountError? {
        var balanceMinusFee = adapter.availableBalance(input.address, feePriority)
        if (balanceMinusFee < BigDecimal.ZERO) {
            balanceMinusFee = BigDecimal.ZERO
        }

        return when (input.inputType) {
            SendModule.InputType.COIN -> {
                SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(adapter.wallet.coin.code, balanceMinusFee)))
            }
            SendModule.InputType.CURRENCY -> {
                exchangeRate?.value?.let {
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

        adapter.send(address, computedAmount, userInput.feePriority)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { delegate?.didSend() },
                        { error ->
                            delegate?.didFailToSend(error)
                        })
                .let {
                    disposables.add(it)
                }
    }

    override fun clear() {
        disposables.clear()
    }

}
