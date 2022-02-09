package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.APIClient
import retrofit2.http.GET
import retrofit2.http.Query

class NftsService(private val accountManager: IAccountManager) {
    private val apiURL = "https://api.opensea.io/api/v1/"

    val xxx = APIClient
        .retrofit(apiURL, 60)
        .create(OpenSeaApiV1::class.java)

    val account by accountManager::activeAccount

}

interface OpenSeaApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") asset_owner: String,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 300,
    ) : List<Map<String, Any>>
}