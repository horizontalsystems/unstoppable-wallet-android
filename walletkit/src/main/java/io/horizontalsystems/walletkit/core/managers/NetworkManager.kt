package io.horizontalsystems.walletkit.core.managers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.Strictness
import io.horizontalsystems.walletkit.core.INetworkManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url
import timber.log.Timber
import java.util.concurrent.TimeUnit

class NetworkManager : INetworkManager {

    override suspend fun getMarkdown(host: String, path: String): String {
        return ServiceGuide.service(host).getGuide(path)
    }

    override suspend fun getReleaseNotes(host: String, path: String): JsonObject {
        return ServiceChangeLogs.service(host).getReleaseNotes(path)
    }

    override suspend fun registerApp(userId: String, referralCode: String)
            : MiniAppRegisterService.RegisterAppResponse {
        return MiniAppRegisterService.service().registerApp(userId, referralCode)
    }

    override suspend fun getWCWhiteList(
        host: String,
        path: String
    ): List<ServiceWCWhitelist.WCWhiteList> {
        return ServiceWCWhitelist.service(host).getWhiteList(path)
    }
}

object ServiceGuide {
    fun service(apiURL: String): GuidesAPI {
        return APIClient.retrofit(apiURL, 60).create(GuidesAPI::class.java)
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

object MiniAppRegisterService {
    private val apiUrl = "https://be.unstoppable.money/"

    fun service(): UnstoppableApi {
        return APIClient.retrofit(apiUrl, 60)
            .create(UnstoppableApi::class.java)
    }

    interface UnstoppableApi {
        @GET("api/v1/tasks/registerApp")
        suspend fun registerApp(
            @Query("userId") userId: String,
            @Query("referralCode") referralCode: String
        ): RegisterAppResponse
    }

    data class RegisterAppResponse(
        val success: Boolean,
        val message: String
    )
}

object ServiceWCWhitelist {
    fun service(apiURL: String): WCWhiteListAPI {
        return APIClient.retrofit(apiURL, 60)
            .create(WCWhiteListAPI::class.java)
    }

    interface WCWhiteListAPI {
        @GET
        @Headers("Content-Type: application/json")
        suspend fun getWhiteList(@Url path: String): List<WCWhiteList>
    }

    data class WCWhiteList(
        val name: String,
        val url: String
    )
}

object APIClient {

    private val logger = HttpLoggingInterceptor { Timber.d(it) }.apply {
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
            .addConverterFactory(
                GsonConverterFactory
                    .create(GsonBuilder().setStrictness(Strictness.LENIENT).create())
            )
            .build()
    }

    //share OkHttpClient
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    val gson by lazy {
        val gsonBuilder = GsonBuilder().setStrictness(Strictness.LENIENT)
        gsonBuilder.create()
    }

    fun retrofit(apiURL: String, timeout: Long = 60): Retrofit {

        val httpClient = okHttpClient.newBuilder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)

        return Retrofit.Builder()
            .baseUrl(apiURL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()
    }
}

