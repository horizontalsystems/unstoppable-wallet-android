package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwapExecutionTypesTest {

    private fun provider(id: String, executionType: String?) =
        UnstoppableAPI.Response.Provider(provider = id, executionType = executionType)

    private val types = SwapExecutionTypes.from(
        listOf(
            provider("MAYACHAIN", "thorchain_deposit"),
            provider("THORCHAIN", "thorchain_deposit"),
            provider("NEAR", "transfer"),
            provider("SOROSWAP", "signed_transaction"),
            provider("PANCAKESWAP", null),
        )
    )

    @Test
    fun `vault-settled providers deliver shielded zcash under either id form`() {
        assertTrue(types.deliversShieldedZcash("u_MAYACHAIN"))
        assertTrue(types.deliversShieldedZcash(MAYA_PROVIDER_ID))
    }

    @Test
    fun `other providers and unknown ids do not`() {
        assertFalse(types.deliversShieldedZcash("u_NEAR"))
        assertFalse(types.deliversShieldedZcash("u_SOROSWAP"))
        assertFalse(types.deliversShieldedZcash(PANCAKE_V3_PROVIDER_ID))
        assertFalse(types.deliversShieldedZcash("u_UNLISTED"))
        assertFalse(SwapExecutionTypes().deliversShieldedZcash("u_MAYACHAIN"))
    }

    @Test
    fun `round-trips through json unchanged`() {
        val gson = Gson()
        val restored = gson.fromJson(gson.toJson(types), SwapExecutionTypes::class.java)

        assertEquals(types, restored)
        assertTrue(restored.deliversShieldedZcash("u_MAYACHAIN"))
    }
}
