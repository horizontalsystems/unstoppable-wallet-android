package bitcoin.wallet

import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.params.TestNet3Params

class WalletManager {

    fun createWallet(words: List<String>) = WalletWrapper(words)

}

class WalletWrapper(words: List<String>) {

    private val masterKey = HDKeyDerivation.createMasterPrivateKey(MnemonicCode.toSeed(words, ""))

    fun getIdentity(): String = Base58.encode(Sha256Hash.hash(masterKey.pubKey))

    fun getPubKeys(): Map<Int, String> {
        return listOf(1, 145).map { it to getPubB58ForCoinType(it) }.toMap()
    }

    private fun getPubB58ForCoinType(coinTypeIndex: Int): String {
        val bip44RootKey = HDKeyDerivation.deriveChildKey(masterKey, ChildNumber(44, true))
        val coinRootKey = HDKeyDerivation.deriveChildKey(bip44RootKey, ChildNumber(coinTypeIndex, true))

        val accountKey = HDKeyDerivation.deriveChildKey(coinRootKey, ChildNumber.ZERO_HARDENED)

        return accountKey.serializePubB58(TestNet3Params())
    }

}