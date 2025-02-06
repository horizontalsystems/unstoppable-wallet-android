package cash.p.terminal.wallet.providers

import androidx.core.text.HtmlCompat
import cash.p.terminal.network.piratenews.domain.repository.PirateNewsRepository
import cash.p.terminal.wallet.models.Post
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import retrofit2.http.GET
import retrofit2.http.Query

class CryptoCompareProvider(
    private val pirateNewsRepository: PirateNewsRepository
) {
    private val baseUrl = "https://min-api.cryptocompare.com/"
    private val newsFeeds = "cointelegraph,theblock,decrypt"
    private val extraParams = "Blocksdecoded"
    private val excludeCategories = "Sponsored"

    private val cryptoCompareService: CryptoCompareService by lazy {
        RetrofitUtils.build(baseUrl).create(CryptoCompareService::class.java)
    }

    fun postsSingle() = Single.merge(
        handleNews(cryptoCompareService.news(excludeCategories, newsFeeds, extraParams)),
        getPirateNews()
    ).toList()
        .map { it.flatten().sortedByDescending { it.timestamp } }

    private fun getPirateNews(): Single<List<Post>> =
        Single.fromCallable {
            runBlocking {
                try {
                    pirateNewsRepository.getNews().map {
                        Post(
                            source = "Pirate.Blog",
                            title = it.title,
                            body = HtmlCompat.fromHtml(it.body, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                .toString(),
                            timestamp = it.date,
                            url = it.link
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }

    private fun handleNews(news: Single<PostsResponse>) =
        news.map { postsResponse ->
            postsResponse.Data.map { postItem ->
                Post(
                    source = postItem.source_info["name"] ?: "",
                    title = postItem.title,
                    body = postItem.body,
                    timestamp = postItem.published_on,
                    url = postItem.url
                )
            }
        }.retryWhenError(Throwable::class)

    interface CryptoCompareService {
        @GET("data/v2/news/")
        fun news(
            @Query("excludedCategories") excludedCategories: String,
            @Query("feeds") feeds: String,
            @Query("extraParams") extraParams: String,
        ): Single<PostsResponse>
    }

    data class PostsResponse(
        val Data: List<PostItem>
    )

    data class PostItem(
        val id: Int,
        val published_on: Long,
        val imageurl: String,
        val title: String,
        val url: String,
        val body: String,
        val source_info: Map<String, String>,
    )
}
