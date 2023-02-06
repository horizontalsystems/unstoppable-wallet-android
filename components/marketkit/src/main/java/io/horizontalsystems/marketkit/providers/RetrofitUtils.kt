package io.horizontalsystems.marketkit.providers

import com.google.gson.GsonBuilder
import io.horizontalsystems.marketkit.HSCache
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtils {

    private fun buildClient(headers: Map<String, String>): OkHttpClient {
        val cache = HSCache.cacheDir?.let {
            Cache(it, HSCache.cacheQuotaBytes)
        }
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)

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
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headersInterceptor)
            .cache(cache)
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

}
