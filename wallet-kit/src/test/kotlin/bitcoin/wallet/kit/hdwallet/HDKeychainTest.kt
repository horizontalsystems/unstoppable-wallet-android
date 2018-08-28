package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.TestHelper
import org.junit.Assert
import org.junit.Test

class HDKeychainTest {

    val mnemonicKeys = listOf("jealous", "digital", "west", "actor", "thunder", "matter", "marble", "marine", "olympic", "range", "dust", "banner")
    val seed = TestHelper.hexToByteArray("6908630f564bd3ca9efb521e72da86727fc78285b15decedb44f40b02474502ed6844958b29465246a618b1b56b4bdffacd1de8b324159e0f7f594c611b0519d")
    val hdKeyManager = HDKeychain(seed)


    @Test
    fun getPrivateKeyByPath_Bip32() {
        var path32 = "m/0"
        val hdKey = hdKeyManager.getKeyByPath(path32)
        val expectedExtendedPrivateKey = "xprv9ubX8LNSQDq5LSMSKyMAmF7rajw2WqonJDcJxYwJmXAApWoQFj4vCg7DZFRrmPVGGc7Jn1oRsX585v2gJqojyVXrseGtk6GccmRDU51fzMX"
        val expectedExtendedPublicKey = "xpub68asXquLEbPNYvRuRztB8P4b8mmWvJXdfSXukwLvKrh9hK8YoGPAkURhQWY9JjmP5Fz8aGMxChcQHKMkfnLWgMaW7LBbnTgTwZb3fVMmfnS"
        Assert.assertEquals(expectedExtendedPrivateKey, hdKey.serializePrivKeyToString())
        Assert.assertEquals(expectedExtendedPublicKey, hdKey.serializePubKeyToString())
    }

    @Test
    fun getPrivateKeyByPath_Bip44() {
        var path44 = "m/44'/0'/0'/0"
        val hdKey = hdKeyManager.getKeyByPath(path44)
        val expectedExtendedPrivateKey = "xprvA1CMgbgebyNq7qBHA9dM1wCUgtgknYKvCnMUAvppfy2WHJTX9oeFHjhVf4FMmQ8KLAfnaGppgVs5EyymtYTm6T1h5wqfHGQURg1NdexvYgY"
        val expectedExtendedPublicKey = "xpub6EBi67DYSLw8LKFkGBAMP59DEvXFC13ma1H4yKESEJZVA6nfhLxVqY1yWM7k7vEkiRQXCzAF1HDQFDF7a8hRmPRXm3x66KTARdeSWUDy9FJ"
        Assert.assertEquals(expectedExtendedPrivateKey, hdKey.serializePrivKeyToString())
        Assert.assertEquals(expectedExtendedPublicKey, hdKey.serializePubKeyToString())
    }

    @Test(expected = NumberFormatException::class)
    @Throws(Exception::class)
    fun getPrivateKeyByPath_invalidPath() {
        var path32 = "m/0/b"
        val hdKey = hdKeyManager.getKeyByPath(path32)
    }

}
