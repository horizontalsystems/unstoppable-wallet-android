package bitcoin.wallet.bitcoin

import com.google.common.collect.ImmutableList
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.KeyCrypter
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed

class Bip44TestNetDeterministicKeyChain(seed: DeterministicSeed, crypter: KeyCrypter? = null) : DeterministicKeyChain(seed, crypter) {

    override fun getAccountPath(): ImmutableList<ChildNumber> =
            ImmutableList.of(ChildNumber(44, true), ChildNumber(1, true), ChildNumber.ZERO_HARDENED)

}
