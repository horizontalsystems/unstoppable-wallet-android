package bitcoin.wallet.modules.send

import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IDatabaseManager

class SendInteractor(private val databaseManager: IDatabaseManager,
                     private var clipboardManager: IClipboardManager,
                     private val adapter: IAdapter) : SendModule.IInteractor {

    var delegate: SendModule.IInteractorDelegate? = null

    override fun getCoinCode(): String {
        return adapter.coin.code
    }

    override fun getBaseCurrency(): String {
        return "USD"
    }

    override fun getCopiedText(): String {
        return clipboardManager.getCopiedText()
    }

    override fun fetchExchangeRate() {
        databaseManager.getExchangeRates().subscribe { rates ->
            val exchangeRate = rates.array.find { it.code == adapter.coin.code }?.value
            exchangeRate?.let { delegate?.didFetchExchangeRate(it) }
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
