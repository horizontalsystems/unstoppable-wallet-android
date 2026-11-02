package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.marketkit.models.BlockchainType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The address vector is the mainnet encoding of a scan/spend key pair produced with ltcd
 * (litecoinmwebkit's MwebAddressTest), not with the handler under test.
 */
class AddressHandlerMwebTest {

    private val handler = AddressHandlerMweb(BlockchainType.LitecoinMweb)
    private val mainnet = "ltcmweb1qqg0xlvas7z37ltr0rmq79f7dl3pfhcz2arr26g5fng6cttkn5q9r2qjm6dv8yym59qgftjpw5lz672l3v7qv2xgksdu9kvrnpysu86j87yec4ejm"
    private val testnet = "tmweb1qqg0xlvas7z37ltr0rmq79f7dl3pfhcz2arr26g5fng6cttkn5q9r2qjm6dv8yym59qgftjpw5lz672l3v7qv2xgksdu9kvrnpysu86j87yc35wwv"

    @Test
    fun acceptsMainnetAddress() {
        assertTrue(handler.isSupported(mainnet))
        val address = handler.parseAddress(mainnet)
        assertEquals(mainnet, address.hex)
        assertEquals(BlockchainType.LitecoinMweb, address.blockchainType)
    }

    @Test
    fun normalizesCase() {
        assertTrue(handler.isSupported(mainnet.uppercase()))
        assertEquals(mainnet, handler.parseAddress(mainnet.uppercase()).hex)
    }

    @Test
    fun rejectsTestnetAddress() {
        assertFalse(handler.isSupported(testnet))
    }

    @Test
    fun rejectsLitecoinAndCorruptedAddresses() {
        assertFalse(handler.isSupported("ltc1qd8yn4z8a3l9q9kqz7hrwhc9k3gdlw2g3xfe3ks"))
        assertFalse(handler.isSupported(mainnet.dropLast(1) + "q"))
        assertFalse(handler.isSupported(""))
    }

    @Test
    fun litecoinChainRegistersTheSameHandlerForPegIns() {
        val litecoin = AddressHandlerMweb(BlockchainType.Litecoin)
        assertEquals(BlockchainType.Litecoin, litecoin.parseAddress(mainnet).blockchainType)
    }
}
