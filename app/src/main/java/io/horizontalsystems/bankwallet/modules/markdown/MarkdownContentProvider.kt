package io.horizontalsystems.bankwallet.modules.markdown

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single
import java.net.URL

class MarkdownPlainContentProvider(
        private val networkManager: INetworkManager,
        private val contentUrl: String
        ) : MarkdownModule.IMarkdownContentProvider {

    override var markdownUrl = contentUrl

    override fun getContent(): Single<String> {
        val url = URL(contentUrl)
        return networkManager.getMarkdown("${url.protocol}://${url.host}", contentUrl)
    }

}

class MarkdownGitReleaseContentProvider(
        private val networkManager: INetworkManager,
        private val contentUrl: String
        ) : MarkdownModule.IMarkdownContentProvider {

    override var markdownUrl = ""

    override fun getContent(): Single<String> {
        val url = URL(contentUrl)
        return networkManager.getReleaseNotes("${url.protocol}://${url.host}", contentUrl)
                .flatMap { jsonObject ->
                    return@flatMap when {
                        jsonObject.has("body") -> Single.just(jsonObject.asJsonObject["body"].asString)
                        else -> Single.just("")
                    }
                }
    }

}
