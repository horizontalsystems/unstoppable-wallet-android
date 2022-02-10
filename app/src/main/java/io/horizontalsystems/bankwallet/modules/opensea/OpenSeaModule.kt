package io.horizontalsystems.bankwallet.modules.opensea

import io.horizontalsystems.bankwallet.core.managers.APIClient
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

object OpenSeaModule {
    private val apiURL = "https://api.opensea.io/api/v1/"

    val apiServiceV1 = APIClient.retrofit(apiURL, 60)
        .create(OpenSeaApiV1::class.java)
}

interface OpenSeaApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : List<Map<String, Any>>

    @Headers(
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    )
    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String,
//        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : Map<String, Any>
}
