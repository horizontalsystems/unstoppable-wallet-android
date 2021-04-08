package io.horizontalsystems.bankwallet.modules.markdown

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.reactivex.Single
import java.net.URL

class MarkdownContentProvider(private val networkManager: INetworkManager) : MarkdownModule.IMarkdownContentProvider {
    override fun getContent(contentUrl: String): Single<String> {
        val url = URL(contentUrl)
        return when (url.protocol) {
            "http", "https" -> networkManager.getMarkdown("${url.protocol}://${url.host}", contentUrl)
            "file" -> {
                val assetFilePath = contentUrl.replace("file:///", "")
                Single.just(
                        App.instance.assets.open(assetFilePath).readBytes().toString(Charsets.UTF_8)
                )
            }
            else -> throw Exception("Invalid protocol: ${url.protocol}")
        }
    }
}
