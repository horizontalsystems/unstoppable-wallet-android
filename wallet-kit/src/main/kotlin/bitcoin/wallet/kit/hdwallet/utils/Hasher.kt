package bitcoin.wallet.kit.hdwallet.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object Hasher {

    fun hash(input: ByteArray, offset: Int, length: Int): ByteArray {
        val digest = newDigest()
        digest.update(input, offset, length)
        return digest.digest()
    }

    fun newDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)  // Can't happen.
        }

    }
}
