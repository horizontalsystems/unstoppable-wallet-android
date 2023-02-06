package io.horizontalsystems.marketkit.providers

import io.horizontalsystems.marketkit.models.Post
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

class CryptoCompareProvider(
    private val apiKey: String? = null
) {
    private val baseUrl = "https://min-api.cryptocompare.com/"
    private val newsFeeds = "cointelegraph,theblock,decrypt"
    private val extraParams = "Blocksdecoded"
    private val excludeCategories = "Sponsored"

    private val cryptoCompareService: CryptoCompareService by lazy {
        RetrofitUtils.build(baseUrl).create(CryptoCompareService::class.java)
    }

    fun postsSingle(): Single<List<Post>> {
        return cryptoCompareService.news(excludeCategories, newsFeeds, extraParams, apiKey)
            .map { postsResponse ->
                postsResponse.Data.map { postItem ->
                    Post(postItem.source_info["name"] ?: "", postItem.title, postItem.body, postItem.published_on, postItem.url)
                }
            }
            .retryWhenError(Throwable::class)
    }

    interface CryptoCompareService {
        @GET("data/v2/news/")
        fun news(
            @Query("excludedCategories") excludedCategories: String,
            @Query("feeds") feeds: String,
            @Query("extraParams") extraParams: String,
            @Query("api_key") apiKey: String? = null
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
        val categories: String,
    )

}
