package bitcoin.wallet.kit.hdwallet;

import bitcoin.wallet.kit.hdwallet.utils.EntropyGenerator
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class MnemonicTest {

    private val entropyGenerator = Mockito.mock(EntropyGenerator::class.java)
    private val mnemonic = Mnemonic(entropyGenerator)

    @Before
    fun setup() {
    }

    @Test
    fun toMnemonic_Success() {
        val entropy: ByteArray = hexStringToByteArray("7787bfe5815e1912a1ec409a56391109")
        val mnemonicWords = mnemonic.toMnemonic(entropy).joinToString(separator = " ")
        val expectedWords = "jealous digital west actor thunder matter marble marine olympic range dust banner"
        Assert.assertEquals(mnemonicWords, expectedWords)
    }

    @Test(expected = Mnemonic.EmptyEntropyException::class)
    @Throws(Exception::class)
    fun toMnemonic_EmptyEntropy() {
        val entropy: ByteArray = hexStringToByteArray("")
        mnemonic.toMnemonic(entropy)
    }

    @Test
    fun validate_Success() {

        val mnemonicKeys = listOf("jealous", "digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        mnemonic.validate(mnemonicKeys)
    }

    @Test(expected = Mnemonic.InvalidMnemonicCountException::class)
    fun validate_WrongWordsCount() {

        val mnemonicKeys = listOf("digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        mnemonic.validate(mnemonicKeys)
    }

    @Test(expected = Mnemonic.InvalidMnemonicKeyException::class)
    fun validate_InvalidMnemonicKey() {

        val mnemonicKeys = listOf("jealous", "digitalll", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        mnemonic.validate(mnemonicKeys)
    }

    @Test
    fun toSeed_Success() {

        val mnemonicKeys = listOf("jealous", "digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        val seed: ByteArray = mnemonic.toSeed(mnemonicKeys)

        val expectedSeed = hexStringToByteArray("6908630f564bd3ca9efb521e72da86727fc78285b15decedb44f40b02474502ed6844958b29465246a618b1b56b4bdffacd1de8b324159e0f7f594c611b0519d")

        Assert.assertArrayEquals(seed, expectedSeed)
    }

    @Test(expected = Mnemonic.InvalidMnemonicCountException::class)
    fun toSeed_WrongWordsCount() {

        val mnemonicKeys = listOf("digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        mnemonic.toSeed(mnemonicKeys)
    }

    @Test(expected = Mnemonic.InvalidMnemonicKeyException::class)
    fun toSeed_InvalidMnemonicKey() {

        val mnemonicKeys = listOf("jealous", "digitalll", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        mnemonic.validate(mnemonicKeys)
    }

    @Test
    fun generate() {

        val entropy: ByteArray = hexStringToByteArray("7787bfe5815e1912a1ec409a56391109")

        whenever(entropyGenerator.getEntropy(any())).thenReturn(entropy)

        val mnemonicKeys = mnemonic.generate().toTypedArray()

        val mnemonicKeysExpected = arrayOf("jealous", "digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")

        Assert.assertArrayEquals(mnemonicKeys, mnemonicKeysExpected)
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)

        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }

        return data
    }

}
