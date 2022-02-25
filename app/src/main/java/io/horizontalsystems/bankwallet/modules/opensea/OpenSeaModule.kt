package io.horizontalsystems.bankwallet.modules.opensea

import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.*
import io.horizontalsystems.marketkit.models.CoinType
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

object OpenSeaModule {
    private val apiURL = "https://api.opensea.io/api/v1/"

    val apiServiceV1 = APIClient.retrofit(apiURL, 60)
        .create(OpenSeaApiV1::class.java)
}

class OpenSeaApiProvider : INftApiProvider {
    override suspend fun getCollectionRecords(
        address: Address,
        account: Account
    ) = fetchCollections(address).map { collectionResponse ->
        NftCollectionRecord(
            accountId = account.id,
            uid = collectionResponse.slug,
            name = collectionResponse.name,
            imageUrl = collectionResponse.image_url,
            stats = NftCollectionStats(
                averagePrice7d = collectionResponse.stats.seven_day_average_price,
                averagePrice30d = collectionResponse.stats.thirty_day_average_price
            )
        )
    }

    override suspend fun getAssetRecords(
        address: Address,
        account: Account
    ) = fetchAssets(address).map { assetResponse ->
        NftAssetRecord(
            accountId = account.id,
            collectionUid = assetResponse.collection.slug,
            tokenId = assetResponse.token_id,
            name = assetResponse.name,
            imageUrl = assetResponse.image_url,
            imagePreviewUrl = assetResponse.image_preview_url,
            description = assetResponse.description ?: "",
            onSale = false,
            lastSale = assetResponse.last_sale?.let { last_sale ->
                NftAssetLastSale(
                    getCoinTypeId(last_sale.payment_token.address),
                    BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                )
            },
            contract = NftAssetContract(
                assetResponse.asset_contract.address,
                assetResponse.asset_contract.schema_name
            )
        )
    }

    private suspend fun fetchAssets(address: Address): List<OpenSeaApiV1Response.Asset> {
        return fetchAllWithLimit(50) { offset, limit ->
            OpenSeaModule.apiServiceV1.assets(address.hex, limit).assets
        }
    }

    private suspend fun fetchCollections(address: Address): List<OpenSeaApiV1Response.Collection> {
        return fetchAllWithLimit(300) { offset, limit ->
            OpenSeaModule.apiServiceV1.collections(address.hex, offset, limit)
        }
    }

    private suspend fun <T> fetchAllWithLimit(limit: Int, f: suspend (Int, Int) -> List<T>): List<T> {
        val assets = mutableListOf<T>()
        var offset = 0
        do {
            val elements = f.invoke(offset, limit)
            assets.addAll(elements)
            offset += limit
        } while (elements.size >= limit)

        return assets
    }

    private fun getCoinTypeId(paymentTokenAddress: String): String {
        val coinType = when (paymentTokenAddress) {
            "0x0000000000000000000000000000000000000000" -> CoinType.Ethereum
            else -> CoinType.Erc20(paymentTokenAddress.lowercase())
        }

        return coinType.id
    }
}

object OpenSeaApiV1Response {
    data class Collection(
        val slug: String,
        val name: String,
        val image_url: String,
        val stats: Stats
    ) {
        data class Stats(
            val seven_day_average_price: BigDecimal,
            val thirty_day_average_price: BigDecimal,
        )
    }

    data class Asset(
        val token_id: String,
        val name: String,
        val image_url: String,
        val image_preview_url: String,
        val description: String?,
        val collection: Collection,
        val last_sale: LastSale?,
        val asset_contract: AssetContract,
    ) {
        data class Collection(
            val slug: String,
        )

        data class LastSale(
            val total_price: BigInteger,
            val payment_token: PaymentToken,
        ) {
            data class PaymentToken(
                val address: String,
                val decimals: Int,
            )
        }

        data class AssetContract(
            val address: String,
            val schema_name: String,
        )
    }

    data class Assets(
        val assets: List<Asset>
    )
}

interface OpenSeaApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : List<OpenSeaApiV1Response.Collection>

    @Headers(
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    )
    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String,
//        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("format") format: String = "json",
    ) : OpenSeaApiV1Response.Assets
}
