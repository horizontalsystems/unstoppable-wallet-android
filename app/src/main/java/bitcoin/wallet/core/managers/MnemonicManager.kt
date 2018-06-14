package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IMnemonic
import org.bitcoinj.core.Utils
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom

class MnemonicManager : IMnemonic {
    override fun generateWords(): List<String> {
        val defautPassphrase = ""
        val deterministicSeed = DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, defautPassphrase, Utils.currentTimeSeconds())
        return deterministicSeed.mnemonicCode ?: listOf()
    }
}
