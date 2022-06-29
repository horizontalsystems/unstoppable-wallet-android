package io.horizontalsystems.bankwallet.modules.showkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.BitcoinCashCoinType
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bitcoincash.MainNetBitcoinCash
import io.horizontalsystems.bitcoinkit.MainNet
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.dashkit.MainNetDash
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.hdwalletkit.HDKeychain
import io.horizontalsystems.litecoinkit.MainNetLitecoin

class ShowKeyViewModel(
    account: Account,
    pinComponent: IPinComponent,
    private val evmBlockchainManager: EvmBlockchainManager
) : ViewModel() {
    private val words: List<String>
    private val seed: ByteArray?

    var passphrase by mutableStateOf("")
        private set

    var viewState by mutableStateOf(ShowKeyModule.ViewState.Warning)
        private set

    var wordsNumbered by mutableStateOf<List<ShowKeyModule.WordNumbered>>(listOf())
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            wordsNumbered = words.mapIndexed { index, word ->
                ShowKeyModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
            seed = account.type.seed
        } else {
            words = listOf()
            seed = null
        }
    }

    val isPinSet = pinComponent.isPinSet

    val ethereumPrivateKey: String
        get() = Signer.privateKey(
            words,
            passphrase,
            evmBlockchainManager.getChain(EvmBlockchain.Ethereum)
        ).toByteArray().let {
            if (it.size > 32) {
                it.copyOfRange(1, it.size)
            } else {
                it
            }.toHexString()
        }

    fun showKey() {
        viewState = ShowKeyModule.ViewState.Key
    }

    fun bitcoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNet()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, purpose(derivation), network.coinType)
    }

    fun bitcoinCashPublicKeys(coinType: BitcoinCashCoinType): String? {
        seed ?: return null
        val keychain = HDKeychain(seed)

        val network = when(coinType){
            BitcoinCashCoinType.type0 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type0)
            BitcoinCashCoinType.type145 -> MainNetBitcoinCash(MainNetBitcoinCash.CoinType.Type145)
        }
        return keysJson(keychain, 44, network.coinType)
    }

    fun litecoinPublicKeys(derivation: AccountType.Derivation): String? {
        seed ?: return null
        val network = MainNetLitecoin()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, purpose(derivation), network.coinType)
    }

    fun dashKeys(): String? {
        seed ?: return null
        val network = MainNetDash()
        val keychain = HDKeychain(seed)

        return keysJson(keychain, 44, network.coinType)
    }

    private fun keysJson(
        keychain: HDKeychain,
        purpose: Int,
        coinType: Int,
    ): String {
        val publicKeys = (0..4).map { accountIndex ->
            val key = keychain.getKeyByPath("m/$purpose'/$coinType'/$accountIndex'")
            key.serializePubB58()
        }
        return publicKeys.toString()
    }

    private fun purpose(derivation: AccountType.Derivation): Int = when (derivation) {
        AccountType.Derivation.bip44 -> 44
        AccountType.Derivation.bip49 -> 49
        AccountType.Derivation.bip84 -> 84
    }

}
