package bitcoin.wallet.modules.send

import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.IClipboardManager

class SendInteractor(private var clipboardManager: IClipboardManager, private val adapter: IAdapter, private val exchangeRateManager: ExchangeRateManager) : SendModule.IInteractor {

    var delegate: SendModule.IInteractorDelegate? = null

    override fun getCoinCode(): String {
        return adapter.coin.code
    }

    override fun getCopiedText(): String {
        return clipboardManager.getCopiedText()
    }

    override fun fetchExchangeRate() {
        val exchangeRates = exchangeRateManager.exchangeRates
        exchangeRates.forEach {
            val (coin, currencyValue) = it
            if (coin.code == adapter.coin.code) {
                delegate?.didFetchExchangeRate(currencyValue.value)
            }
        }
    }

    override fun send(coinCode: String, address: String, amount: Double) {
        try {
            adapter.send(address, (amount * 100_000_000).toInt())
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
