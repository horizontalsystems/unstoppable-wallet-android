package io.horizontalsystems.bankwallet.modules.coin.tweets

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

class TweetsProvider(private val bearerToken: String?) {
    interface TwitterAPI {
        @GET("users/by")
        fun getUsers(
            @Query("usernames") usernames: String,
            @Query("user.fields") userFields: String,
            @Header("Authorization") authHeader: String,
        ): Single<UsersResponse>

        @GET("users/{userId}/tweets")
        fun getTweets(
            @Path("userId") userId: String,
            @Query("expansions") expansions: String,
            @Query("media.fields") mediaFields: String,
            @Query("tweet.fields") tweetFields: String,
            @Query("user.fields") userFields: String,
            @Query("max_results") maxResults: Int,
            @Header("Authorization") authHeader: String,
            ): Single<TweetsPageResponse>
    }

    data class UsersResponse(val data: List<TwitterUser>)

    class UserNotFound : Exception()

    private val baseUrl = "https://api.twitter.com/2/"
    private val service = APIClient.retrofit(baseUrl, 60, true).create(TwitterAPI::class.java)

    fun userRequestSingle(username: String): Single<TwitterUser> {
        return service.getUsers(username, "profile_image_url", "Bearer $bearerToken")
            .map { it.data }
            .flatMap {
                when {
                    it.isNotEmpty() -> Single.just(it.first())
                    else -> Single.error(UserNotFound())
                }
            }
    }


    fun tweetsSingle(user: TwitterUser): Single<List<Tweet>> {
        return service
            .getTweets(
                userId = user.id,
                expansions = "attachments.poll_ids,attachments.media_keys,referenced_tweets.id,referenced_tweets.id.author_id",
                mediaFields = "media_key,preview_image_url,type,url",
                tweetFields = "id,author_id,created_at,attachments",
                userFields = "profile_image_url",
                maxResults = 50,
                authHeader = "Bearer $bearerToken"
            )
            .map {
                it.tweets(user)
            }
    }


}

