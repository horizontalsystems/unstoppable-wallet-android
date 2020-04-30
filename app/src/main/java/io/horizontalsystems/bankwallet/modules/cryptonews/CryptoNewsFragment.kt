package io.horizontalsystems.bankwallet.modules.cryptonews

import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.views.inflate
import io.horizontalsystems.xrateskit.entities.CryptoNews
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_crypto_news.*
import kotlinx.android.synthetic.main.view_holder_crypto_news.*

class CryptoNewsFragment(private val coinCode: String) : Fragment() {

    private lateinit var presenter: CryptoNewsPresenter
    private lateinit var cryptoNewsView: CryptoNewsView
    private lateinit var adapter: CryptoNewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_crypto_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter = ViewModelProvider(this, CryptoNewsModule.Factory(coinCode)).get(CryptoNewsPresenter::class.java)
        cryptoNewsView = presenter.view as CryptoNewsView
        adapter = CryptoNewsAdapter()

        newsRecyclerView.adapter = adapter
        observeEvents()

        presenter.onLoad()
    }

    private fun observeEvents() {
        cryptoNewsView.showNews.observe(this, Observer { items ->
            adapter.items = items
            adapter.notifyDataSetChanged()
        })

        cryptoNewsView.showSpinner.observe(this, Observer { show ->
            spinner.visibility = if (show) View.VISIBLE else View.GONE
            notAvailable.visibility = View.GONE
        })

        cryptoNewsView.showError.observe(this, Observer {
            notAvailable.visibility = View.VISIBLE
        })
    }
}

class CryptoNewsAdapter : RecyclerView.Adapter<ViewHolderNews>() {

    var items = listOf<CryptoNews>()

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderNews {
        return ViewHolderNews(inflate(parent, R.layout.view_holder_crypto_news))
    }

    override fun onBindViewHolder(holder: ViewHolderNews, position: Int) {
        holder.bind(items[position])
    }
}

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
