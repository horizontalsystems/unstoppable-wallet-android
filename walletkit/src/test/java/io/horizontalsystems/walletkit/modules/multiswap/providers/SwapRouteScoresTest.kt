package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwapRouteScoresTest {

    private fun provider(id: String, amlPolicy: String?) =
        UnstoppableAPI.Response.Provider(provider = id, amlPolicy = amlPolicy)

    private val scores = SwapRouteScores.from(
        listOf(
            provider("THORCHAIN", "excellent"),
            provider("QUICKEX", "good"),
            provider("NEAR", "fair"),
            provider("PANCAKESWAP", "GOOD"),
            provider("SWAPUZ", "unheard-of"),
            provider("LIZEX", null),
        )
    )

    @Test
    fun `maps the server policy to a risk level under either id form`() {
        assertEquals(RiskLevel.EXCELLENT, scores.riskLevel("u_THORCHAIN"))
        assertEquals(RiskLevel.GOOD, scores.riskLevel("u_QUICKEX"))
        assertEquals(RiskLevel.FAIR, scores.riskLevel("u_NEAR"))
        assertEquals(RiskLevel.GOOD, scores.riskLevel(PANCAKE_V3_PROVIDER_ID))
    }

    @Test
    fun `unknown providers and values have no score`() {
        assertNull(scores.riskLevel("u_SWAPUZ"))
        assertNull(scores.riskLevel("u_LIZEX"))
        assertNull(scores.riskLevel("u_UNLISTED"))
        assertNull(SwapRouteScores().riskLevel("u_THORCHAIN"))
    }

    @Test
    fun `round-trips through json unchanged`() {
        val gson = Gson()
        val restored = gson.fromJson(gson.toJson(scores), SwapRouteScores::class.java)

        assertEquals(scores, restored)
        assertEquals(RiskLevel.EXCELLENT, restored.riskLevel("u_THORCHAIN"))
    }
}
