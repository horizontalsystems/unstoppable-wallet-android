package io.horizontalsystems.walletkit.modules.multiswap.providers

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the provider-id half of the suspension contract against a captured `GET /providers`
 * response. A rule can only be enforced if the id it is published under resolves to the id the app
 * offers that provider by, and nothing else in the app compares the two — an id the server spells
 * differently produces no error, just a provider that quietly cannot be suspended.
 *
 * The fixture is deliberately trimmed to ids and rules; it is not a snapshot of live policy, and
 * this test asserts nothing about which rules happen to be configured.
 */
class SwapProviderIdMappingTest {

    private val responses: List<UnstoppableAPI.Response.Provider> = Gson().fromJson(
        javaClass.classLoader!!.getResource("providers-dev.json")!!.readText(),
        Array<UnstoppableAPI.Response.Provider>::class.java,
    ).toList()

    // The registry cannot be built in a unit test — its providers reach for App at construction —
    // so the ids are listed from their declaration sites.
    private val appProviderIds = UProvider.entries.map { "u_${it.id}" } + listOf(
        "allbridge",
        "u_STELLARBROKER",
        THORCHAIN_PROVIDER_ID,
        MAYA_PROVIDER_ID,
        ONEINCH_PROVIDER_ID,
        UNISWAP_V3_PROVIDER_ID,
        PANCAKE_V3_PROVIDER_ID,
    )

    @Test
    fun `every provider the app offers resolves to a server provider id`() {
        val serverIds = responses.map { it.provider }.toSet()

        val unmatched = appProviderIds.filterNot { SwapSuspensionIndex.serverId(it) in serverIds }

        assertEquals(listOf<String>(), unmatched)
    }
}
