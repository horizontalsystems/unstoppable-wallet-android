package io.horizontalsystems.bankwallet.core.managers

import android.annotation.SuppressLint
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Flowable
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class NetworkManager : INetworkManager {

    override suspend fun getMarkdown(host: String, path: String): String {
        return ServiceGuide.service(host).getGuide(path)
    }

    override suspend fun getReleaseNotes(host: String, path: String): JsonObject {
        return ServiceChangeLogs.service(host).getReleaseNotes(path)
    }

    override fun getTransaction(host: String, path: String, isSafeCall: Boolean): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host, isSafeCall).getFullTransaction(path)
    }

    override fun getTransactionWithPost(host: String, path: String, body: Map<String, Any>): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host)
            .getFullTransactionWithPost(path, body.mapValues { it.value.toString() })
    }

    override fun ping(host: String, url: String, isSafeCall: Boolean): Flowable<Any> {
        return ServicePing.service(host, isSafeCall).ping(url)
    }

    override fun getEvmInfo(host: String, path: String): Single<JsonObject> {
        return ServiceEvmContractInfo.service(host).getTokenInfo(path)
    }

    override suspend fun getBep2TokeInfo(blockchainUid: String, symbol: String): TokenInfoService.Bep2TokenInfo {
        return TokenInfoService.service().getBep2TokenInfo(blockchainUid, symbol)
    }

    override suspend fun getEvmTokeInfo(blockchainUid: String, address: String): TokenInfoService.EvmTokenInfo {
        return TokenInfoService.service().getEip20TokenInfo(blockchainUid, address)
    }
}

object ServiceFullTransaction {
    fun service(apiURL: String, isSafeCall: Boolean = true): FullTransactionAPI {
        return APIClient.retrofit(apiURL, 60, isSafeCall)
            .create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        @Headers("Content-Type: application/json")
        fun getFullTransaction(@Url path: String): Flowable<JsonObject>

        @POST
        @Headers("Content-Type: application/json")
        fun getFullTransactionWithPost(@Url path: String, @Body body: Map<String, String>): Flowable<JsonObject>
    }

}

object ServicePing {
    fun service(apiURL: String, isSafeCall: Boolean = true): FullTransactionAPI {
        return APIClient.retrofit(apiURL, timeout = 8, isSafeCall = isSafeCall).create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        fun ping(@Url path: String): Flowable<Any>
    }
}

object ServiceEvmContractInfo {
    fun service(apiURL: String): EvmContractInfoAPI {
        return APIClient.retrofit(apiURL, 60)
            .create(EvmContractInfoAPI::class.java)
    }

    interface EvmContractInfoAPI {
        @GET
        @Headers("Content-Type: application/json")
        fun getTokenInfo(@Url path: String): Single<JsonObject>
    }

}

object TokenInfoService {
    private val apiUrl = "${App.appConfigProvider.marketApiBaseUrl}/v1/token_info/"

    fun service(): TokenInfoAPI {
        return APIClient.retrofit(apiUrl, 60)
            .create(TokenInfoAPI::class.java)
    }

    data class Bep2TokenInfo(
        val name: String,
        @SerializedName("original_symbol")
        val originalSymbol: String,
        @SerializedName("contract_decimals")
        val decimals: Int
    )

    data class EvmTokenInfo(
        val name: String,
        val symbol: String,
        val decimals: Int
    )

    interface TokenInfoAPI {
        @GET("bep2")
        suspend fun getBep2TokenInfo(
            @Query("blockchain") blockchain: String,
            @Query("symbol") symbol: String
        ): Bep2TokenInfo

        @GET("eip20")
        suspend fun getEip20TokenInfo(
            @Query("blockchain") blockchain: String,
            @Query("address") address: String
        ): EvmTokenInfo
    }
}

object ServiceGuide {
    fun service(apiURL: String): GuidesAPI {
        return APIClient.retrofit(apiURL, 60, true).create(GuidesAPI::class.java)
    }

    interface GuidesAPI {
        @GET
        suspend fun getGuide(@Url path: String): String
    }
}

object ServiceChangeLogs {
    fun service(apiURL: String): ChangeLogsAPI {
        return APIClient.retrofit(apiURL, 60)
            .create(ChangeLogsAPI::class.java)
    }

    interface ChangeLogsAPI {

        @GET
        @Headers("Content-Type: application/json")
        suspend fun getReleaseNotes(@Url path: String): JsonObject
    }
}

object APIClient {

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private fun buildClient(headers: Map<String, String>): OkHttpClient {
        val headersInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            headers.forEach { (name, value) ->
                requestBuilder.header(name, value)
            }
            chain.proceed(requestBuilder.build())
        }

        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .addInterceptor(headersInterceptor)
            .build()
    }

    fun build(baseUrl: String, headers: Map<String, String> = mapOf()): Retrofit {
        val client = buildClient(headers)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().setLenient().create())
            )
            .build()
    }

    //share OkHttpClient
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    fun retrofit(apiURL: String, timeout: Long = 60, isSafeCall: Boolean = true): Retrofit {

        val httpClient = okHttpClient.newBuilder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)

        //TODO Replace this implementation with Manifest file settings when support for SDK 26 removed
        if (!isSafeCall) // if host name cannot be verified, has no or self signed certificate, do unsafe request
            setUnsafeSocketFactory(httpClient)

        val gsonBuilder = GsonBuilder().setLenient()

        return Retrofit.Builder()
            .baseUrl(apiURL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
            .client(httpClient.build())
            .build()
    }

    @SuppressLint("TrustAllX509TrustManager", "BadHostnameVerifier")
    private fun setUnsafeSocketFactory(builder: OkHttpClient.Builder) {
        try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            builder.sslSocketFactory(sslSocketFactory, (trustAllCerts[0] as X509TrustManager))
            builder.hostnameVerifier(HostnameVerifier { _, _ -> true })
            builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
            builder.readTimeout(60000, TimeUnit.MILLISECONDS)

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}
