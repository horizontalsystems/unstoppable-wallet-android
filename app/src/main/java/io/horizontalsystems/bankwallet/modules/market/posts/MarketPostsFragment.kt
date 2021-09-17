package io.horizontalsystems.bankwallet.modules.market.posts

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.MarketLoadingAdapter
import io.horizontalsystems.core.helpers.HudHelper
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

        val loadingAdapter = MarketLoadingAdapter(
            viewModel.loadingLiveData,
            viewModel.errorLiveData,
            viewModel::onErrorClick,
            viewLifecycleOwner
        )

        val postsAdapter = MarketPostItemsAdapter(
            this,
            viewModel.postsViewItemsLiveData,
            viewLifecycleOwner
        )

        marketPostsRecyclerView.adapter = ConcatAdapter(loadingAdapter, postsAdapter)

        pullToRefresh.setOnRefreshListener {
            viewModel.refresh()

            Handler(Looper.getMainLooper()).postDelayed({
                pullToRefresh.isRefreshing = false
            }, 1500)
        }

        viewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }
    }

    override fun onPostClick(postViewItem: MarketPostsModule.PostViewItem) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        context?.let {
            customTabsIntent.launchUrl(it, Uri.parse(postViewItem.url))
        }
    }
}
