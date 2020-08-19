package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.*
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.reactivex.Single
import java.io.InputStream
import java.lang.reflect.Type
import java.net.URL
import java.util.*


object GuidesManager {

    private val guidesUrl = App.appConfigProvider.guidesUrl

    private val gson = GsonBuilder()
            .setDateFormat("dd-MM-yyyy")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Guide::class.java, GuideDeserializer(guidesUrl))
            .create()

    fun getGuideCategories(): Single<Array<GuideCategory>> {
        return Single.fromCallable {
            URL(guidesUrl)
                    .openConnection()
                    .apply {
                        connectTimeout = 5000
                        readTimeout = 60000
                        setRequestProperty("Accept", "application/json")
                    }.getInputStream()
                    .use {
                        gson.fromJson(it.bufferedReader(), Array<GuideCategory>::class.java)
                    }
        }
    }

    fun getGuideContent(fileUrl: String): Single<String> {
        val url = URL(fileUrl)
        val host = "${url.protocol}://${url.host}"

        return App.networkManager.getGuide(host, fileUrl)
    }


    class GuideDeserializer(guidesUrl: String) : JsonDeserializer<Guide> {
        private val guidesUrlObj = URL(guidesUrl)

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Guide {
            val jsonObject = json.asJsonObject

            return Guide(
                    jsonObject.get("title").asString,
                    context.deserialize(jsonObject.get("updated_at"), Date::class.java),
                    jsonObject["image_url"].asString?.let { absolutify(it) },
                    absolutify(jsonObject["file_url"].asString)
            )
        }

        private fun absolutify(relativeUrl: String?): String {
            return URL(guidesUrlObj, relativeUrl).toString()
        }
    }
}
