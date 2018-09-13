package bitcoin.wallet.core.managers

//class MnemonicManager : IMnemonic {

//    override fun generateWords(): List<String> {
//        val defautPassphrase = ""
//        val deterministicSeed = DeterministicSeed(SecureRandom(), DeterministicSeed.DEFAULT_SEED_ENTROPY_BITS, defautPassphrase, Utils.currentTimeSeconds())
//        return deterministicSeed.mnemonicCode ?: listOf()
//    }
//
//    override fun validateWords(words: List<String>) = try {
//        MnemonicCode.INSTANCE.check(words)
//        true
//    } catch (e: MnemonicException) {
//        false
//    }

//}
