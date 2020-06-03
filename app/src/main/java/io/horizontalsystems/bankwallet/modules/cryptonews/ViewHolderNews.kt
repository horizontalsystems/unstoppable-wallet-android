package io.horizontalsystems.bankwallet.modules.cryptonews

import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.xrateskit.entities.CryptoNews
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_crypto_news.*

class ViewHolderNews(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    fun bind(item: CryptoNews) {
        newsTitle.text = item.title
        newsTime.text = DateUtils.getRelativeTimeSpanString(item.timestamp * 1000)

        containerView.setOnClickListener {
            loadNews(item.url)
        }
    }

    private fun loadNews(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(containerView.context, Uri.parse(url))
    }
}
