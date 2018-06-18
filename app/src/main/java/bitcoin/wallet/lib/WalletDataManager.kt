package bitcoin.wallet.lib

import bitcoin.wallet.core.App
import bitcoin.wallet.core.NetworkManager
import bitcoin.wallet.entities.Transaction
import bitcoin.wallet.modules.transactions.IAddressesProvider
import bitcoin.wallet.modules.transactions.ITransactionsDataProvider
import io.reactivex.Flowable
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom

object WalletDataManager : IAddressesProvider, ITransactionsDataProvider {

    private val defautPassphrase = ""

    var mnemonicWords: List<String>
        get() = App.preferences.getString("mnemonicWords", "").split(", ").filter { it.isNotBlank() }
        set(value) {
            App.preferences.edit().putString("mnemonicWords", value.joinToString(", ")).apply()
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
        for (i in 0..20) {
            val externalKeyI = HDKeyDerivation.deriveChildKey(external, i)

            val addressExternal = externalKeyI.toAddress(networkParameters).toString()

            val changeKeyI = HDKeyDerivation.deriveChildKey(change, i)

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
