package io.horizontalsystems.bankwallet.modules.markdown

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single
import java.lang.Exception
import java.net.URL

class MarkdownContentProvider(private val networkManager: INetworkManager) : MarkdownModule.IMarkdownContentProvider {
    override fun getContent(contentUrl: String): Single<String> {
        val url = URL(contentUrl)
        return when(url.protocol){
            "http", "https" -> networkManager.getMarkdown("${url.protocol}://${url.host}", contentUrl)
            else -> throw Exception("Invalid protocol")
        }
    }
}
