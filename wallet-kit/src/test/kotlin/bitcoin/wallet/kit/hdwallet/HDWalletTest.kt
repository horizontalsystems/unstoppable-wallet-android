package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.TestHelper
import org.junit.Assert
import org.junit.Test

class HDWalletTest {

    private val seed = TestHelper.hexToByteArray("6908630f564bd3ca9efb521e72da86727fc78285b15decedb44f40b02474502ed6844958b29465246a618b1b56b4bdffacd1de8b324159e0f7f594c611b0519d")
    private val hdWallet = HDWallet(seed)

    @Test
    fun receiveAddress_correctAddress() {
        val publicKey = hdWallet.receiveAddress(0)
        val expectedAddress = "188TR7fL2MpqoAMez2VgLgsjZoRcttZvAb"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun receiveAddress_correctPublicKey() {
        val publicKey = hdWallet.receiveAddress(0)
        val expectedPublicKey = "031f4e92f8d1f78d8a149863415690b2c2845fcae3be009f9d55595e4edc00e2ea"
        Assert.assertEquals(expectedPublicKey, TestHelper.byteArrayToHex(publicKey.publicKey))
    }

    @Test
    fun changeAddress_correctAddress() {
        val publicKey = hdWallet.changeAddress(0)
        val expectedAddress = "14T7kwGvdxrEHgUA28BbcddJi7j4fhWy7Z"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun changeAddress_correctPublicKey() {
        val publicKey = hdWallet.changeAddress(0)
        val expectedPublicKey = "0301aeeb78a8ee9201659fcbe8d78e73205e7b26e0e46608e2a661aabe87822ce5"
        Assert.assertEquals(expectedPublicKey, TestHelper.byteArrayToHex(publicKey.publicKey))
    }

}
