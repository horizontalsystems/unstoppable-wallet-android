package io.horizontalsystems.solanakit.transactions

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.solanakit.models.TokenAccount
import io.horizontalsystems.solanakit.models.TokenInfo
import io.reactivex.Single
import kotlinx.coroutines.rx2.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.math.BigDecimal
import java.util.logging.Logger

class SolanaFmService {

    private val baseUrl = "https://api.solana.fm/v1/"
    private val logger = Logger.getLogger("SolanaFmService")

    private val api: SolanaFmApi
    private val gson: Gson

    init {
        val loggingInterceptor = HttpLoggingInterceptor { message -> logger.info(message) }
            .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()

        api = retrofit.create(SolanaFmApi::class.java)
    }

    suspend fun tokenAccounts(address: String): List<TokenAccount> {
        val response = api.legacyTokenAccounts(address).await()

        return response.tokens.values.map { token ->
            TokenAccount(token.ata, token.mint, token.balance.movePointRight(token.tokenData.decimals), token.tokenData.decimals)
        }
    }

    suspend fun tokenInfo(mintAddress: String): TokenInfo {
        val response = api.tokenInfo(mintAddress).await()

        return TokenInfo(
            name = response.tokenDetails.name,
            symbol = response.tokenDetails.symbol,
            decimals = response.decimals
        )
    }

    private interface SolanaFmApi {
        @GET("addresses/{address}/tokens?tokenType=Legacy")
        fun legacyTokenAccounts(
            @Path("address") address: String
        ): Single<TokenAccountsResponse>

        @GET("tokens/{mintAddress}")
        fun tokenInfo(
            @Path("mintAddress") mintAddress: String
        ): Single<TokenInfoResponse>
    }

    data class TokenInfoResponse(
        val mint: String,
        val decimals: Int,
        @SerializedName("tokenList")
        val tokenDetails: TokenInfoDetails
    )

    data class TokenInfoDetails(
        val name: String,
        val symbol: String
    )

    data class TokenAccountsResponse(
        val pubkey: String,
        val tokens: Map<String, TokenResponse>,
    )

    data class TokenResponse(
        val mint: String,
        val ata: String,
        val balance: BigDecimal,
        val tokenData: TokenData
    )

    data class TokenData(
        val tokenType: String,
        val decimals: Int,
        val mintAuthority: String,
        val freezeAuthority: String,
    )

}
