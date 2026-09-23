package io.horizontalsystems.walletkit.chain.evm

import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.modules.manageaccount.evmprivatekey.PrivateKeyPage
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class EvmChainPluginPrivateKeyTest {

    private val plugin = EvmChainPlugin(BlockchainType.Ethereum)

    @Test
    fun privateKeyRows_keepsLeadingZeroByte() {
        val hex = "00123456789abcdef0112233445566778899aabbccddeeff0011223344556677"

        assertEquals(hex, privateKeyShown(hex))
    }

    @Test
    fun privateKeyRows_dropsSignByte() {
        val hex = "f1e2d3c4b5a69788796a5b4c3d2e1f00112233445566778899aabbccddeeff01"

        assertEquals(hex, privateKeyShown(hex))
    }

    private fun privateKeyShown(hex: String): String {
        val account = Account(
            id = "id",
            name = "name",
            type = AccountType.EvmPrivateKey(BigInteger(hex, 16)),
            origin = AccountOrigin.Restored,
            level = 0,
        )
        val page = plugin.privateKeyRows(account).single().page as PrivateKeyPage
        return page.input.privateKey
    }
}
