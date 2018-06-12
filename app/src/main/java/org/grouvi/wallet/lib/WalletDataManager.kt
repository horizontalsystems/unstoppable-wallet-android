package org.grouvi.wallet.lib

import android.util.Log
import io.reactivex.Flowable
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.*
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.core.App
import org.grouvi.wallet.entities.Coin
import org.grouvi.wallet.modules.backupWords.BackupWordsModule
import org.grouvi.wallet.modules.restoreWallet.RestoreWalletModule
import org.grouvi.wallet.modules.wallet.WalletModule
import java.security.SecureRandom

object WalletDataManager : BackupWordsModule.IWordsProvider, RestoreWalletModule.IWalletRestorer, WalletModule.ICoinsDataProvider {

    private val defautPassphrase = ""

    override var mnemonicWords: List<String>
        get() = App.preferences.getString("mnemonicWords", "").split(", ").filter { it.isNotBlank() }
        set(value) {
            App.preferences.edit().putString("mnemonicWords", value.joinToString(", ")).apply()
        }

    override fun restoreWallet(words: List<String>)  {
        try {
            MnemonicCode.INSTANCE.check(words)

            mnemonicWords = words
            masterKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(words, defautPassphrase))

        } catch (e: MnemonicException) {
            throw RestoreWalletModule.InvalidWordsException()
        }
    }

    override fun getCoins(): Flowable<List<Coin>> {
        return Flowable.just(listOf(
                Coin("Bitcoin", "BTC", 1.0, 7000.0, 7000.0),
                Coin("Bitcoin Cash", "BCH", 1.0, 1000.0, 1000.0),
                Coin("Ethereum", "ETH", 2.0, 600.0, 1200.0)
        ))
    }

    fun hasWallet(): Boolean {
        return mnemonicWords.isNotEmpty()
    }

    fun createWallet() {
        // todo
        Log.e("AAA", "Creating wallet...")

        generateSeed(defautPassphrase)
        createMasterKey()
        deriveBip44RootKey()
        deriveBitcoinRootKey()

    }

    ////////////

    private var seed: DeterministicSeed? = null
    private var masterKey: DeterministicKey? = null
    private var bip44RootKey: DeterministicKey? = null
    private var bitcoinRootKey: DeterministicKey? = null

    fun generateSeed(passphrase: String) {
        seed = DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, passphrase, Utils.currentTimeSeconds())

        seed?.mnemonicCode?.let {
            mnemonicWords = it
        }
    }

    fun createMasterKey() {
        masterKey = HDKeyDerivation.createMasterPrivateKey(seed?.seedBytes)
    }

    fun deriveBip44RootKey() {
        bip44RootKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(44, true))
    }

    fun deriveBitcoinRootKey() {
        bitcoinRootKey = HDKeyDerivation.deriveChildKey(bip44RootKey, ChildNumber(0, true))
    }

}
