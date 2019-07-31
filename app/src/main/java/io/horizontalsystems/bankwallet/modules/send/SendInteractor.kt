package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal



class SendInteractor(private val adapter: IAdapter) : SendModule.IInteractor {

    sealed class SendError : Exception() {
        class NoAddress : SendError()
        class NoAmount : SendError()
    }

    var delegate: SendModule.IInteractorDelegate? = null

    override val coin: Coin
        get() = adapter.wallet.coin

    private var validateDisposable: Disposable? = null
    private var feeDisposable: Disposable? = null

    override fun parsePaymentAddress(address: String): PaymentRequestAddress {
        return adapter.parsePaymentAddress(address)
    }

    @Throws
    override fun getAvailableBalance(params: Map<SendModule.AdapterFields, Any?>): BigDecimal {
        return adapter.availableBalance(params)
    }

    override fun validate(params: Map<SendModule.AdapterFields, Any?>) {
        validateDisposable?.dispose()

        validateDisposable = Single.fromCallable { adapter.validate(params) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { errorList -> delegate?.onValidationComplete(errorList) },
                        { /*exception*/ }
                )
    }

    override fun updateFee(params: Map<SendModule.AdapterFields, Any?>) {
        feeDisposable?.dispose()

        feeDisposable = Single.fromCallable { adapter.fee(params) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { fee ->
                            delegate?.onFeeUpdated(fee)
                        },
                        { error ->
                            /*exception*/
                        }
                )
    }

    //
//    override fun stateForUserInput(input: SendModule.UserInput): SendModule.State {
//
//        val coin = adapter.wallet.coin.code
//        val baseCurrency = currencyManager.baseCurrency
//        val rateValue = exchangeRate?.value
//
//        val decimal = if (input.inputType == SendModule.InputType.COIN) Math.min(adapter.decimal, appConfigProvider.maxDecimal) else appConfigProvider.fiatDecimal
//
//        val state = SendModule.State(decimal, input.inputType)
//
//        state.address = input.address
//        val address = input.address
//
//        if (address != null) {
//            try {
//                adapter.validate(address)
//            } catch (e: Exception) {
//                state.addressError = SendModule.AddressError.InvalidAddress()
//            }
//        }
//
//        when (input.inputType) {
//            SendModule.InputType.COIN -> {
//                state.coinValue = CoinValue(coin, input.amount)
//                rateValue?.let {
//                    state.currencyValue = CurrencyValue(baseCurrency, input.amount.times(it))
//                }
//            }
//            SendModule.InputType.CURRENCY -> {
//                state.currencyValue = CurrencyValue(baseCurrency, input.amount)
//                rateValue?.let {
//                    state.coinValue = CoinValue(coin, input.amount.divide(it, 8, RoundingMode.HALF_EVEN))
//                }
//            }
//        }
//
//        val errors = adapter.validate(state.coinValue?.value
//                ?: BigDecimal.ZERO, address, input.feePriority)
//
//        for (error in errors) {
//            when (error) {
//                SendStateError.InsufficientAmount -> state.amountError = getAmountError(input, input.feePriority)
//                SendStateError.InsufficientFeeBalance -> state.feeError = getFeeError(input, input.feePriority)
//            }
//        }
//
//        state.coinValue?.let { coinValue ->
//            val coinCode = adapter.feeCoinCode ?: adapter.wallet.coin.code
//            if (coinValue.value > BigDecimal.ZERO) {
//                state.feeCoinValue = CoinValue(coinCode, adapter.fee(coinValue.value, input.address, input.feePriority))
//            } else {
//                state.feeCoinValue = CoinValue(coinCode, BigDecimal.ZERO)
//            }
//        }
//
//        var feeCurrencyRate: BigDecimal? = null
//        adapter.feeCoinCode?.let {
//            feeCurrencyRate = exchangeFeeRate?.value
//        } ?: run {
//            feeCurrencyRate = rateValue
//        }
//
//        feeCurrencyRate?.let { feeCurRate ->
//            state.feeCoinValue?.let { feeCoinValue ->
//                state.feeCurrencyValue = CurrencyValue(baseCurrency, feeCoinValue.value.times(feeCurRate))
//            }
//        }
//
//        return state
//    }

//    private fun getFeeError(input: SendModule.UserInput, feePriority: FeeRatePriority): SendModule.AmountError.Erc20FeeError? {
//        adapter.feeCoinCode?.let {
//            val fee = adapter.fee(input.amount, input.address, feePriority)
//            val coinValue = CoinValue(it, fee)
//            return SendModule.AmountError.Erc20FeeError(adapter.wallet.coin.code, coinValue)
//        } ?: return null
//    }

//    private fun getAmountError(input: SendModule.UserInput, feePriority: FeeRatePriority): SendModule.AmountError? {
//        var balanceMinusFee = adapter.availableBalance(input.address, feePriority)
//        if (balanceMinusFee < BigDecimal.ZERO) {
//            balanceMinusFee = BigDecimal.ZERO
//        }
//
//        return when (input.inputType) {
//            SendModule.InputType.COIN -> {
//                SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CoinValueInfo(CoinValue(adapter.wallet.coin.code, balanceMinusFee)))
//            }
//            SendModule.InputType.CURRENCY -> {
//                exchangeRate?.value?.let {
//                    val currencyBalanceMinusFee = balanceMinusFee * it
//                    SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currencyManager.baseCurrency, currencyBalanceMinusFee)))
//                }
//            }
//        }
//    }

    override fun send(userInput: SendModule.UserInput) {
        val address = userInput.address
        if (address == null) {
            delegate?.didFailToSend(SendError.NoAddress())
            return
        }

//        val computedAmount = when (userInput.inputType) {
//            SendModule.InputType.COIN -> userInput.amount
//            SendModule.InputType.CURRENCY -> convertedAmountForInputType(SendModule.InputType.CURRENCY, userInput.amount)
//        }
//
//        if (computedAmount == null || computedAmount.compareTo(BigDecimal.ZERO) == 0) {
//            delegate?.didFailToSend(SendError.NoAmount())
//            return
//        }
//
//        adapter.send(address, computedAmount, userInput.feePriority)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        { delegate?.didSend() },
//                        { error ->
//                            delegate?.didFailToSend(error)
//                        })
//                .let {
//                    disposables.add(it)
//                }
    }

    override fun clear() {
        validateDisposable?.dispose()
        feeDisposable?.dispose()
    }

}
