package bitcoin.wallet.kit.hdwallet

import org.junit.Test

class HDKeychainTest {

    val mnemonicKeys = listOf("jealous", "digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")
    val seed = hexStringToByteArray("6908630f564bd3ca9efb521e72da86727fc78285b15decedb44f40b02474502ed6844958b29465246a618b1b56b4bdffacd1de8b324159e0f7f594c611b0519d")
    val hdKeyManager = HDKeychain(seed)


    @Test
    fun getPrivateKeyFromPath() {
        var path32 = "m/0"
        hdKeyManager.getKeyByPath(path32)

        var path44 = "m/44'/0'/0'/0"
        hdKeyManager.getKeyByPath(path44)
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
