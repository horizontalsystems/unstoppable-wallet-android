package bitcoin.wallet.modules.send

import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.IClipboardManager
import bitcoin.wallet.core.IDatabaseManager

class SendInteractor(private val databaseManager: IDatabaseManager,
                     private var blockChainManager: BlockchainManager,
                     private var clipboardManager: IClipboardManager,
                     private val coinCode: String) : SendModule.IInteractor {

    var delegate: SendModule.IInteractorDelegate? = null

    override fun getBaseCurrency(): String {
        return "USD"
    }

    override fun getCopiedText(): String {
        return clipboardManager.getCopiedText()
    }

    override fun fetchExchangeRate() {
        databaseManager.getExchangeRates().subscribe {
            val exchangeRate = it.array.find { it.code == coinCode }?.value
            exchangeRate?.let { delegate?.didFetchExchangeRate(it) }
        }
    }

    override fun send(coinCode: String, address: String, amount: Double) {
        try {
            blockChainManager.sendCoins(coinCode, address, (amount * 100_000_000).toLong())
            delegate?.didSend()
        } catch (exception: Exception) {
            delegate?.didFailToSend(exception)
        }
    }
}
