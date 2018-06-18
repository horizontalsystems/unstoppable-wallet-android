package bitcoin.wallet.lib

import bitcoin.wallet.core.App
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.entities.Coin
import bitcoin.wallet.entities.Transaction
import bitcoin.wallet.log
import bitcoin.wallet.modules.transactions.IAddressesProvider
import bitcoin.wallet.modules.transactions.ITransactionsDataProvider
import bitcoin.wallet.modules.wallet.WalletModule
import io.reactivex.Flowable
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom

object WalletDataManager :
        WalletModule.ICoinsDataProvider, IAddressesProvider, ITransactionsDataProvider {

    private val defautPassphrase = ""

    var mnemonicWords: List<String>
        get() = App.preferences.getString("mnemonicWords", "").split(", ").filter { it.isNotBlank() }
        set(value) {
            App.preferences.edit().putString("mnemonicWords", value.joinToString(", ")).apply()
        }


    override fun getCoins(): Flowable<List<Coin>> {

        return NetworkManager.getUnspentOutputs()
                .map {

                    val bitcoinAmount = it.map { it.value }.sum() / 100000000.0
                    val bitcoinExchangeRate = 7000.0

                    listOf(
                            Coin("Bitcoin", "BTC", bitcoinAmount, bitcoinAmount * bitcoinExchangeRate, bitcoinExchangeRate),
                            Coin("Bitcoin Cash", "BCH", 1.0, 1000.0, 1000.0),
                            Coin("Ethereum", "ETH", 2.0, 600.0, 1200.0)
                    )
                }

    }

    override fun getAddresses(): List<String> {

        val masterKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(mnemonicWords, defautPassphrase))
        val bip44RootKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(44, true))
        val bitcoinRootKey = HDKeyDerivation.deriveChildKey(bip44RootKey, ChildNumber(if (App.testMode) 1 else 0, true))

        val bitcoinAccountKey = HDKeyDerivation.deriveChildKey(bitcoinRootKey, ChildNumber.ZERO_HARDENED)

        val external = HDKeyDerivation.deriveChildKey(bitcoinAccountKey, ChildNumber.ZERO)
        val change = HDKeyDerivation.deriveChildKey(bitcoinAccountKey, ChildNumber.ONE)

        val networkParameters = when (App.testMode) {
            true -> TestNet3Params()
            else -> MainNetParams()
        }

        val addresses = mutableListOf<String>()
        for (i in 0..1) {
            val externalKeyI = HDKeyDerivation.deriveChildKey(external, i)

            externalKeyI.pathAsString.log("External Key Path")

            val addressExternal = externalKeyI.toAddress(networkParameters).toString()

            val changeKeyI = HDKeyDerivation.deriveChildKey(change, i)

            changeKeyI.pathAsString.log("Change Key Path")

            val addressChange = changeKeyI.toAddress(networkParameters).toString()
            addresses.add(addressExternal)
            addresses.add(addressChange)
        }

        return addresses
    }

    override fun getTransactions(): Flowable<List<Transaction>> {
        return NetworkManager.getTransactions()
    }

    fun hasWallet(): Boolean {
        return mnemonicWords.isNotEmpty()
    }

    fun createWallet() {
        DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, defautPassphrase, Utils.currentTimeSeconds()).mnemonicCode?.let {
            mnemonicWords = it
        }
    }

}
