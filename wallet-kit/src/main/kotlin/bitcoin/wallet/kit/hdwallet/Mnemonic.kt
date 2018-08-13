package bitcoin.wallet.kit.hdwallet

import WordList
import bitcoin.wallet.kit.hdwallet.utils.PBKDF2SHA512
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import kotlin.experimental.and


class Mnemonic {

    private val PBKDF2_ROUNDS = 2048

    enum class Strength(val value: Int) {
        Default(128),
        Low(160),
        Medium(192),
        High(224),
        VeryHigh(256)
    }

    /**
     * Generate mnemonic keys
     */
    fun generate(strength: Strength = Strength.Default): List<String> {
        val seed = ByteArray(strength.value / 8)
        val random = SecureRandom()
        random.nextBytes(seed)
        return toMnemonic(seed)
    }

    /**
     * Convert entropy data to mnemonic word list.
     */
    fun toMnemonic(entropy: ByteArray): List<String> {
        if (entropy.isEmpty())
            throw Mnemonic.EmptyEntropyException("Entropy is empty.")

        // We take initial entropy of ENT bits and compute its
        // checksum by taking first ENT / 32 bits of its SHA256 hash.

        val hashed = hash(entropy, 0, entropy.size)
        val hashBits = bytesToBits(hashed)

        val entropyBits = bytesToBits(entropy)
        val checksumLengthBits = entropyBits.size / 32

        // We append these bits to the end of the initial entropy.
        val concatBits = BooleanArray(entropyBits.size + checksumLengthBits)
        System.arraycopy(entropyBits, 0, concatBits, 0, entropyBits.size)
        System.arraycopy(hashBits, 0, concatBits, entropyBits.size, checksumLengthBits)

        // Next we take these concatenated bits and split them into
        // groups of 11 bits. Each group encodes number from 0-2047
        // which is a position in a wordlist.  We convert numbers into
        // words and use joined words as mnemonic sentence.

        val wordList = WordList.getWords()
        val words = ArrayList<String>()
        val nwords = concatBits.size / 11
        for (i in 0 until nwords) {
            var index = 0
            for (j in 0..10) {
                index = index shl 1
                if (concatBits[i * 11 + j])
                    index = index or 0x1
            }
            words.add(wordList[index])
        }

        return words
    }


    /**
     * Convert mnemonic keys to seed
     */
    fun toSeed(mnemonicKeys: List<String>): ByteArray {

        validate(mnemonicKeys)

        // To create binary seed from mnemonic, we use PBKDF2 function
        // with mnemonic sentence (in UTF-8) used as a password and
        // string "mnemonic" + passphrase (again in UTF-8) used as a
        // salt. Iteration count is set to 2048 and HMAC-SHA512 is
        // used as a pseudo-random function. Desired length of the
        // derived key is 512 bits (= 64 bytes).
        //
        val pass = mnemonicKeys.joinToString(separator = " ")
        val salt = "mnemonic"

        return PBKDF2SHA512.derive(pass, salt, PBKDF2_ROUNDS, 64)
    }


    /**
     * Validate mnemonic keys
     */
    fun validate(mnemonicKeys: List<String>) {

        if (mnemonicKeys.size !in (12..24 step 3)) {
            throw InvalidMnemonicCountException("Count: ${mnemonicKeys.size}")
        }

        val wordsList = WordList.getWords()

        for (mnemonic: String in mnemonicKeys) {
            if (!wordsList.contains(mnemonic))
                throw InvalidMnemonicKeyException("Invalid word: $mnemonic")
        }

    }

    private fun bytesToBits(data: ByteArray): BooleanArray {
        val bits = BooleanArray(data.size * 8)
        for (i in data.indices)
            for (j in 0..7) {
                val tmp1 = 1 shl (7 - j)
                val tmp2 = data[i] and tmp1.toByte()

                bits[i * 8 + j] = tmp2 != 0.toByte()
            }
        return bits
    }

    private fun hash(input: ByteArray, offset: Int, length: Int): ByteArray {
        val digest = newDigest()
        digest.update(input, offset, length)
        return digest.digest()
    }

    private fun newDigest(): MessageDigest {
        try {
            return MessageDigest.getInstance("SHA-256")
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)  // Can't happen.
        }

    }

    open class MnemonicException(message: String) : Exception(message)

    class EmptyEntropyException(message: String) : MnemonicException(message)

    class InvalidMnemonicCountException(message: String) : MnemonicException(message)

    class InvalidMnemonicKeyException(message: String) : MnemonicException(message)

}
