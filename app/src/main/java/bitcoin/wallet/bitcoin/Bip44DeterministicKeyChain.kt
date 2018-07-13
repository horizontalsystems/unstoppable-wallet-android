package bitcoin.wallet.bitcoin

import com.google.common.collect.ImmutableList
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.KeyCrypter
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed

class Bip44DeterministicKeyChain(seed: DeterministicSeed, crypter: KeyCrypter? = null) : DeterministicKeyChain(seed, crypter) {

    override fun getAccountPath(): ImmutableList<ChildNumber> = BIP44_ACCOUNT_ZERO_PATH

}
