package org.grouvi.wallet.lib

import android.util.Log
import io.reactivex.Flowable
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.*
import org.bitcoinj.wallet.DeterministicSeed
import org.grouvi.wallet.core.App
import org.grouvi.wallet.entities.Coin
import org.grouvi.wallet.entities.Transaction
import org.grouvi.wallet.entities.TransactionInput
import org.grouvi.wallet.entities.TransactionOutput
import org.grouvi.wallet.modules.backupWords.BackupWordsModule
import org.grouvi.wallet.modules.restoreWallet.RestoreWalletModule
import org.grouvi.wallet.modules.transactions.IAddressesProvider
import org.grouvi.wallet.modules.transactions.ITransactionsDataProvider
import org.grouvi.wallet.modules.wallet.WalletModule
import java.security.SecureRandom

object WalletDataManager :
        BackupWordsModule.IWordsProvider,
        RestoreWalletModule.IWalletRestorer,
        WalletModule.ICoinsDataProvider, IAddressesProvider, ITransactionsDataProvider {

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
        // todo replace stub
        return Flowable.just(listOf(
                Coin("Bitcoin", "BTC", 1.0, 7000.0, 7000.0),
                Coin("Bitcoin Cash", "BCH", 1.0, 1000.0, 1000.0),
                Coin("Ethereum", "ETH", 2.0, 600.0, 1200.0)
        ))
    }

    override fun getAddresses(): List<String> {
        // todo replace stub
        return listOf("3GsfBQ6Df4tofeqvsGid4GAyUjn82tRE77", "38PUuTYFoJMmkz7sfidHbKCp5Rxz2rbTZG")
    }

    override fun getTransactions(): Flowable<List<Transaction>> {
        // todo replace stub
        val transactions = listOf(
                Transaction().apply {
                    inputs = listOf(
                            TransactionInput("34gyGHhoCBbWJPsCrCwdUjRHDa7rihTUGa", 6989776510)
                    )
                    outputs = listOf(
                            TransactionOutput("3GsfBQ6Df4tofeqvsGid4GAyUjn82tRE77", 6989776501),
                            TransactionOutput("3K8jbcHrLAkQ1t2zfsbgBM48w37AL22JRC", 9)
                    )
                },
                Transaction().apply {
                    inputs = listOf(
                            TransactionInput("3GsfBQ6Df4tofeqvsGid4GAyUjn82tRE77", 6989776501)
                    )
                    outputs = listOf(
                            TransactionOutput("1HN8SMYNXu4dLQxijYNP8gb5NfUMaCRecD", 980000000),
                            TransactionOutput("38PUuTYFoJMmkz7sfidHbKCp5Rxz2rbTZG", 6009660301)
                    )
                }
        )
        return Flowable.just(transactions)
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
