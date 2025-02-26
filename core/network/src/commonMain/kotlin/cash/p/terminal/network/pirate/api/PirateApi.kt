package cash.p.terminal.network.pirate.api

import cash.p.terminal.network.api.parseResponse
import cash.p.terminal.network.pirate.data.entity.MasterNodesDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url

internal class PirateApi(
    private val httpClient: HttpClient
) {
    private companion object {
        const val PIRATE_BASE_PLACE_URL = "https://pirate.cash/s1/"
    }

    suspend fun getCoinInfo(): MasterNodesDto {
        return httpClient.get {
            url(PIRATE_BASE_PLACE_URL + "dash/masternodes.json")
        }.parseResponse()
    }
}