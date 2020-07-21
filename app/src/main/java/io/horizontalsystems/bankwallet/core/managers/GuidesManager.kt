package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

object GuidesManager {

    private val guidesUrl = App.appConfigProvider.guidesUrl
    private val httpClient = OkHttpClient()
    private val gson = GsonBuilder()
            .setDateFormat("dd-MM-yyyy")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

    fun getGuideCategories(): Single<Array<GuideCategory>> {
        return Single.fromCallable {
            val request = Request.Builder()
                    .url(guidesUrl)
                    .build()

            val response = httpClient.newCall(request).execute()
            val categories = gson.fromJson(response.body?.charStream(), Array<GuideCategory>::class.java)
            response.close()

            categories.map { category ->
                GuideCategory(category.title).apply {
                    this.guides = category.guides.map {
                        val imageUrl = it.imageUrl?.let { absolutify(it) }
                        val fileUrl = absolutify(it.fileUrl)
                        Guide(it.title, it.updatedAt, imageUrl, fileUrl)
                    }
                }
            }.toTypedArray()
        }
    }

    private fun absolutify(relativeUrl: String) : String {
        return URL(URL(guidesUrl), relativeUrl).toString()
    }

    fun getGuideContent(fileUrl: String): Single<String> {
        val url = URL(fileUrl)
        val host = "${url.protocol}://${url.host}"

        return App.networkManager.getGuide(host, fileUrl)
    }
}
