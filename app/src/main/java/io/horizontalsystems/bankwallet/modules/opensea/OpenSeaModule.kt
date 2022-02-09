package io.horizontalsystems.bankwallet.modules.opensea

import io.horizontalsystems.bankwallet.core.managers.APIClient
import retrofit2.http.GET
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
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 300,
    ) : List<Map<String, Any>>
}
