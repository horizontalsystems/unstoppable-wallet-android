package org.grouvi.wallet.lib

import android.util.Log
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.core.App
import org.grouvi.wallet.modules.backupWords.BackupWordsModule
import java.security.SecureRandom

object WalletDataManager : BackupWordsModule.IWordsProvider {

    override var mnemonicWords: List<String>
        get() = App.preferences.getString("mnemonicWords", "").split(", ").filter { it.isNotBlank() }
        set(value) {
            App.preferences.edit().putString("mnemonicWords", value.joinToString(", ")).apply()
        }

    fun hasWallet(): Boolean {
        return mnemonicWords.isNotEmpty()
    }

    fun createWallet() {
        // todo
        Log.e("AAA", "Creating wallet...")

        generateSeed("")
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
