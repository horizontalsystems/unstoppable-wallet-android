package bitcoin.wallet.bitcoin

import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.KeyCrypter
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.wallet.DefaultKeyChainFactory
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.Protos

class Bip44KeyChainFactory(private val params: NetworkParameters) : DefaultKeyChainFactory() {

    override fun makeKeyChain(key: Protos.Key, firstSubKey: Protos.Key, seed: DeterministicSeed, crypter: KeyCrypter?, isMarried: Boolean): DeterministicKeyChain {
        return if (isMarried) {
            super.makeKeyChain(key, firstSubKey, seed, crypter, isMarried)
        } else if (params is TestNet3Params) {
            Bip44TestNetDeterministicKeyChain(seed, crypter)
        } else if (params is MainNetParams) {
            Bip44DeterministicKeyChain(seed, crypter)
        } else {
            super.makeKeyChain(key, firstSubKey, seed, crypter, isMarried)
        }

    }

//    @Throws(UnreadableWalletException::class)
//    override fun makeWatchingKeyChain(key: Protos.Key, firstSubKey: Protos.Key, accountKey: DeterministicKey,
//                                      isFollowingKey: Boolean, isMarried: Boolean): DeterministicKeyChain {
//
//        if (accountKey.path != DeterministicKeyChain.ACCOUNT_ZERO_PATH)
//            throw UnreadableWalletException("Expecting account key but found key with path: " + HDUtils.formatPath(accountKey.path))
//
//        return when {
//            isMarried -> super.makeWatchingKeyChain(key, firstSubKey, accountKey, isFollowingKey, isMarried)
//            else -> DeterministicKeyChain(accountKey, isFollowingKey)
//        }
//
//    }
}