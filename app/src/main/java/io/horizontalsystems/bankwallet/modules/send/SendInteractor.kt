package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IClipboardManager
import io.horizontalsystems.bankwallet.core.IExchangeRateManager

class SendInteractor(private var clipboardManager: IClipboardManager, private val adapter: IAdapter, private val exchangeRateManager: IExchangeRateManager) : SendModule.IInteractor {

    var delegate: SendModule.IInteractorDelegate? = null

    override fun getCoinCode(): String {
        TODO()
//        return adapter.coin.code
    }

    override fun getCopiedText(): String {
        return clipboardManager.getCopiedText()
    }

    override fun fetchExchangeRate() {
        val exchangeRates = exchangeRateManager.getExchangeRates()
        exchangeRates.forEach {
            val (coin, currencyValue) = it
//            if (coin.code == adapter.coin.code) {
//                delegate?.didFetchExchangeRate(currencyValue.value)
//            }
        }
    }

    override fun send(address: String, amount: Double) {
        try {
            adapter.send(address, amount)
            delegate?.didSend()
        } catch (exception: Exception) {
            delegate?.didFailToSend(exception)
        }
    }

    override fun isValid(address: String): Boolean {
        //todo add address validation
        return true
    }
}
