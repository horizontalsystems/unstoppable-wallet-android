package bitcoin.wallet.blockchain

import android.security.keystore.UserNotAuthenticatedException
import bitcoin.wallet.core.ILocalStorage
import javax.inject.Inject

class BlockchainManager @Inject constructor(private val bitcoinBlockchainService: IBlockchainService, private val localStorage: ILocalStorage) {

    private val blockchainServices = mapOf<String, IBlockchainService>(
            "BTC" to bitcoinBlockchainService
    )

    fun startServices() {
        localStorage.savedWords?.let {
            bitcoinBlockchainService.start(it)
        }
    }

    @Throws(UserNotAuthenticatedException::class)
    fun initNewWallet(words: List<String>) {
        localStorage.saveWords(words)
        bitcoinBlockchainService.initNewWallet()
    }

    @Throws(NotEnoughFundsException::class, InvalidAddress::class)
    fun sendCoins(coinCode: String, address: String, value: Long) {
        getServiceByCoinCode(coinCode).sendCoins(address, value)
    }

    private fun getServiceByCoinCode(coinCode: String) =
            blockchainServices[coinCode] ?: throw UnsupportedBlockchain(coinCode)

    @Throws(Exception::class)
    fun getReceiveAddress(coinCode: String): String {
        return getServiceByCoinCode(coinCode).getReceiveAddress()
    }

}

interface IBlockchainService {

    fun sendCoins(address: String, value: Long)

    fun getReceiveAddress(): String

    fun start(paperKeys: List<String>)

    fun initNewWallet()

}

class NotEnoughFundsException(cause: Throwable) : Exception(cause)

class InvalidAddress(cause: Throwable) : Exception(cause)

class UnsupportedBlockchain(coinCode: String) : Exception("Unsupported blockchain $coinCode")