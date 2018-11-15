package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.Coin

class SendInteractor(private val currencyManager: ICurrencyManager,
                     private val rateManager: RateManager,
                     private val clipboardManager: IClipboardManager,
                     private val wallet: Wallet) : SendModule.IInteractor {

    var delegate: SendModule.IInteractorDelegate? = null

    override val coin: Coin
        get() = wallet.coin

    override val addressFromClipboard: String?
        get() = clipboardManager.getCopiedText()

    override fun convertedAmountForInputType(inputType: SendModule.InputType, amount: Double): Double? {
        val rate = rateManager.rate(wallet.coin, currencyManager.baseCurrency.code) ?: return null

        return when (inputType) {
            SendModule.InputType.COIN -> amount * rate.value
            SendModule.InputType.CURRENCY -> amount / rate.value
        }
    }

    override fun stateForUserInput(input: SendModule.UserInput): SendModule.State {

        val coin = wallet.coin
        val adapter = wallet.adapter
        val baseCurrency = currencyManager.baseCurrency
        val rateValue = rateManager.rate(coin, baseCurrency.code)?.value

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
                        SendModule.AmountError.InsufficientBalance(SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(baseCurrency, currencyBalance)))
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

        val rateValue = rateManager.rate(wallet.coin, currencyManager.baseCurrency.code)?.value
                ?: return

        val address = userInput.address ?: return

        val amount = if (userInput.inputType == SendModule.InputType.COIN) userInput.amount else userInput.amount / rateValue

        wallet.adapter.send(address, amount) { error ->
            when (error) {
                null -> delegate?.didSend()
                else -> delegate?.didFailToSend(error)
            }
        }
    }
}
