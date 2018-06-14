package bitcoin.wallet.core.managers

import bitcoin.wallet.core.IMnemonic
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.MnemonicCode
import org.bitcoinj.crypto.MnemonicException
import org.bitcoinj.wallet.DeterministicSeed
import java.security.SecureRandom

class MnemonicManager : IMnemonic {

    override fun generateWords(): List<String> {
        val defautPassphrase = ""
        val deterministicSeed = DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, defautPassphrase, Utils.currentTimeSeconds())
        return deterministicSeed.mnemonicCode ?: listOf()
    }

    override fun validateWords(words: List<String>) = try {
        MnemonicCode.INSTANCE.check(words)
        true
    } catch (e: MnemonicException) {
        false
    }

}
