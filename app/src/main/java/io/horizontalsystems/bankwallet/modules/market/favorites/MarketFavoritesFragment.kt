package io.horizontalsystems.bankwallet.modules.market.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketItemsAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.ViewHolderMarketItem
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_market_favorites.*

class MarketFavoritesFragment : BaseFragment(), MarketListHeaderView.Listener, ViewHolderMarketItem.Listener {

    private val marketListViewModel by viewModels<MarketListViewModel> { MarketFavoritesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this
        marketListViewModel.marketViewItemsLiveData.observe(viewLifecycleOwner, { (list, _) ->
            marketListHeader.isVisible = list.isNotEmpty()
        })

        val marketItemsAdapter = MarketItemsAdapter(
            this,
            marketListViewModel.marketViewItemsLiveData,
            marketListViewModel.loadingLiveData,
            marketListViewModel.errorLiveData,
            viewLifecycleOwner
        )

        val emptyListAdapter =
            EmptyListAdapter(marketListViewModel.showEmptyListTextLiveData, viewLifecycleOwner) { parent, viewType ->
                EmptyFavoritesViewHolder.create(parent, viewType)
            }

        coinRatesRecyclerView.adapter = ConcatAdapter(marketItemsAdapter, emptyListAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setProgressBackgroundColorSchemeResource(R.color.claude)
        pullToRefresh.setColorSchemeResources(R.color.oz)

        pullToRefresh.setOnRefreshListener {
            marketListViewModel.refresh()
        }

        marketListViewModel.loadingLiveData.observe(viewLifecycleOwner, { loading ->
            pullToRefresh.isRefreshing = loading
        })

        errorViewCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        errorViewCompose.setContent {
            ComposeAppTheme {
                ListErrorView(stringResource(R.string.Market_SyncError)) {
                    marketListViewModel.onErrorClick()
                }
            }
        }

        marketListViewModel.errorLiveData.observe(viewLifecycleOwner) { error ->
            errorViewCompose.isVisible = error != null
        }

        marketListViewModel.topMenuLiveData.observe(viewLifecycleOwner) { (sortMenu, toggleButton) ->
            marketListHeader.setMenu(sortMenu, toggleButton)
        }
    }

    override fun onSortingClick() {
        val items = marketListViewModel.getSortingMenuItems()

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketListViewModel.sortingFields[position]
                    marketListViewModel.updateSorting(selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onToggleButtonClick() {
        marketListViewModel.onToggleButtonClick()
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    class EmptyFavoritesViewHolder(containerView: View) : RecyclerView.ViewHolder(containerView) {
        companion object {
            fun create(parent: ViewGroup, viewType: Int): EmptyFavoritesViewHolder {
                return EmptyFavoritesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_empty_favorites_list, parent, false))
            }
        }
    }
}
