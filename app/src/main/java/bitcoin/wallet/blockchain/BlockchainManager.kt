package bitcoin.wallet.blockchain

import android.content.res.AssetManager
import bitcoin.wallet.bitcoin.BitcoinBlockchainService
import java.io.File

object BlockchainManager {

    fun init(filesDir: File, assetManager: AssetManager, storage: BlockchainStorage, testMode: Boolean) {
        BitcoinBlockchainService.init(filesDir, assetManager, storage, testMode)
    }

    fun startServices() {
        BitcoinBlockchainService.start()
    }

    fun initNewWallet() {
        BitcoinBlockchainService.initNewWallet()
    }

    @Throws(NotEnoughFundsException::class, InvalidAddress::class)
    fun sendCoins(coinCode: String, address: String, value: Long) {
        BitcoinBlockchainService.sendCoins(address, value)
    }

}

class NotEnoughFundsException(cause: Throwable) : Exception(cause)

class InvalidAddress(cause: Throwable) : Exception(cause)
