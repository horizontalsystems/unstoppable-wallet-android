package bitcoin.wallet.kit.hdwallet.utils

import java.security.SecureRandom

class EntropyGenerator {

    fun getEntropy(bits: Int): ByteArray {
        val random = SecureRandom()
        val seed = ByteArray(bits / 8)
        random.nextBytes(seed)
        return seed
    }

}