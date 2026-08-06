package io.horizontalsystems.walletkit

import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.thorchainkit.ThorchainKit
import io.horizontalsystems.thorchainkit.network.Network
import org.junit.Assert.assertEquals
import org.junit.Test

class ThorchainAddressDerivationTest {

    // BIP39 reference mnemonic, path m/44'/931'/0'/0/0
    @Test
    fun deriveAddressFromKnownMnemonic() {
        val words = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about".split(" ")
        val seed = Mnemonic().toSeed(words, "")

        val address = ThorchainKit.getAddress(seed, Network.Mainnet).toString()

        // expected value cross-checked with an independent BIP32/secp256k1/bech32 implementation
        assertEquals("thor1gm00vwsfcp48enm4uv9e5dhm37jtd0ye27wrx0", address)
    }
}
