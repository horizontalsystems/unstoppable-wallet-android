package cash.p.terminal.network.piratenews.api

import cash.p.terminal.network.api.parseResponse
import cash.p.terminal.network.piratenews.data.entity.PiratePostItemDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url

internal class PirateNewsApi(
    private val httpClient: HttpClient
) {
    private companion object {
        const val PIRATE_NEWS_BASE_PLACE_URL = "https://pirate.blog/"
    }

    suspend fun getNews(): List<PiratePostItemDto> {
        return httpClient.get {
            url(PIRATE_NEWS_BASE_PLACE_URL + "wp-json/wp/v2/posts")
        }.parseResponse()
    }
}