package bitcoin.wallet.blockchain

import android.content.res.AssetManager
import bitcoin.wallet.bitcoin.BitcoinBlockchainService
import java.io.File

object BlockchainManager {

    private val blockchainServices = mapOf<String, IBlockchainService>(
            "BTC" to BitcoinBlockchainService
    )

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
        getServiceByCoinCode(coinCode).sendCoins(address, value)
    }

    private fun getServiceByCoinCode(coinCode: String) =
            blockchainServices[coinCode] ?: throw UnsupportedBlockchain(coinCode)

    fun getReceiveAddress(coinCode: String): String {
        return getServiceByCoinCode(coinCode).getReceiveAddress()
    }

}

interface IBlockchainService {

    fun sendCoins(address: String, value: Long)

    fun getReceiveAddress(): String

}

class NotEnoughFundsException(cause: Throwable) : Exception(cause)

class InvalidAddress(cause: Throwable) : Exception(cause)

class UnsupportedBlockchain(coinCode: String) : Exception("Unsupported blockchain $coinCode")