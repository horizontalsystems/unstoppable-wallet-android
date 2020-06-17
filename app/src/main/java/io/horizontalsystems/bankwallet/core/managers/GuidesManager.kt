package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Single
import okhttp3.*

object GuidesManager {

    private val httpClient = OkHttpClient()

    private val gson = GsonBuilder()
            .setDateFormat("dd-MM-yyyy")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    fun getGuideCategories(): Single<Array<GuideCategory>> {
        return Single.fromCallable {
            val request = Request.Builder()
                    .url("https://raw.githubusercontent.com/horizontalsystems/blockchain-crypto-guides/master/index.json")
                    .build()

            val response = httpClient.newCall(request).execute()
            val categories = gson.fromJson(response.body?.charStream(), Array<GuideCategory>::class.java)
            response.close()

            categories
        }
    }
}
