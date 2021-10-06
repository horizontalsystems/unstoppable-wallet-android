package io.horizontalsystems.bankwallet.modules.market.posts

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import kotlinx.android.synthetic.main.fragment_market_posts.*

class MarketPostsFragment : BaseFragment(), ViewHolderMarketPostItem.Listener {

    private val viewModel by viewModels<MarketPostsViewModel> { MarketPostsModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_market_posts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketPostsRecyclerView.adapter = MarketPostItemsAdapter(
            this,
            viewModel.postsViewItemsLiveData,
            viewLifecycleOwner
        )

        pullToRefresh.setOnRefreshListener {
            viewModel.refresh()
        }
        pullToRefresh.setProgressBackgroundColorSchemeResource(R.color.claude)
        pullToRefresh.setColorSchemeResources(R.color.oz)

        viewModel.loadingLiveData.observe(viewLifecycleOwner) {
            pullToRefresh.isRefreshing = it
        }

        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            error.isVisible = it != null
            error.text = it
        }
        error.setOnSingleClickListener {
            viewModel.onErrorClick()
        }

        viewModel.postsViewItemsLiveData.observe(viewLifecycleOwner) {
            marketPostsRecyclerView.isVisible = it.isNotEmpty()
        }
    }

    override fun onPostClick(postViewItem: MarketPostsModule.PostViewItem) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        context?.let {
            customTabsIntent.launchUrl(it, Uri.parse(postViewItem.url))
        }
    }
}
