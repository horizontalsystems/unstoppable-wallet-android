package io.horizontalsystems.walletkit.chain.evm

import io.horizontalsystems.marketkit.models.BlockchainType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest
import java.security.Security

class EvmChainPluginSecurityProviderTest {

    @Test
    fun construction_makesKeccakAvailable() {
        Security.removeProvider("BC")

        EvmChainPlugin(BlockchainType.Ethereum)

        val hash = MessageDigest.getInstance("ETH-KECCAK-256").digest(ByteArray(0))
        assertEquals(
            "c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470",
            hash.joinToString("") { "%02x".format(it) },
        )
    }
}
