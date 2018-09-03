package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.TestHelper
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.TestNet
import org.junit.Assert
import org.junit.Test

class HDWalletTest {

    private val seed = TestHelper.hexToByteArray("6908630f564bd3ca9efb521e72da86727fc78285b15decedb44f40b02474502ed6844958b29465246a618b1b56b4bdffacd1de8b324159e0f7f594c611b0519d")
    private val hdWalletMainNet = HDWallet(seed, MainNet())
    private val hdWalletTestNet = HDWallet(seed, TestNet())

    @Test
    fun receiveAddress_correctAddress_mainNet() {
        val publicKey = hdWalletMainNet.receiveAddress(0)
        val expectedAddress = "188TR7fL2MpqoAMez2VgLgsjZoRcttZvAb"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun receiveAddress_correctAddress_testNet() {
        val publicKey = hdWalletTestNet.receiveAddress(0)
        val expectedAddress = "myKrd9zwDqGbzawnEzKERBgj3GMafkrdKv"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun receiveAddress_correctPublicKey_mainNet() {
        val publicKey = hdWalletMainNet.receiveAddress(0)
        val expectedPublicKey = "031f4e92f8d1f78d8a149863415690b2c2845fcae3be009f9d55595e4edc00e2ea"
        Assert.assertEquals(expectedPublicKey, TestHelper.byteArrayToHex(publicKey.publicKey))
    }

    @Test
    fun receiveAddress_correctPublicKey_testNet() {
        val publicKey = hdWalletTestNet.receiveAddress(0)
        val expectedPublicKey = "035e028c6d6b0f18d31d699957f219e75415c2f5dea979f3f4771e11954ec77c13"
        Assert.assertEquals(expectedPublicKey, TestHelper.byteArrayToHex(publicKey.publicKey))
    }

    @Test
    fun changeAddress_correctAddress() {
        val publicKey = hdWalletMainNet.changeAddress(0)
        val expectedAddress = "14T7kwGvdxrEHgUA28BbcddJi7j4fhWy7Z"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun changeAddress_correctAddress_testNet() {
        val publicKey = hdWalletTestNet.changeAddress(0)
        val expectedAddress = "my3ZdJWxn5L8wMWkaTVrYHjTz8RUzDfkwf"
        Assert.assertEquals(expectedAddress, publicKey.address)
    }

    @Test
    fun changeAddress_correctPublicKey() {
        val publicKey = hdWalletMainNet.changeAddress(0)
        val expectedPublicKey = "0301aeeb78a8ee9201659fcbe8d78e73205e7b26e0e46608e2a661aabe87822ce5"
        Assert.assertEquals(expectedPublicKey, TestHelper.byteArrayToHex(publicKey.publicKey))
    }

}
