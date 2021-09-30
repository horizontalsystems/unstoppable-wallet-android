package io.horizontalsystems.bankwallet.modules.market.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_discovery.*
import kotlinx.android.synthetic.main.fragment_market_discovery.coinRatesRecyclerView
import kotlinx.android.synthetic.main.fragment_market_discovery.marketListHeader
import kotlinx.android.synthetic.main.fragment_market_discovery.pullToRefresh

class MarketDiscoveryFragment : BaseFragment(), MarketListHeaderView.Listener, ViewHolderMarketItem.Listener, MarketCategoriesAdapter.Listener {

    private val vmFactory = MarketDiscoveryModule.Factory()

    private val marketDiscoveryViewModel by viewModels<MarketDiscoveryViewModel> { vmFactory }
    private val marketListViewModel by viewModels<MarketListViewModel> { vmFactory }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_discovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this

        val marketItemsAdapter = MarketItemsAdapter(
                this,
                marketListViewModel.marketViewItemsLiveData,
                marketListViewModel.loadingLiveData,
                marketListViewModel.errorLiveData,
                viewLifecycleOwner
        )
        val marketLoadingAdapter = MarketLoadingAdapter(
                marketListViewModel.loadingLiveData,
                marketListViewModel.errorLiveData,
                marketListViewModel::onErrorClick,
                viewLifecycleOwner
        )

        coinRatesRecyclerView.adapter = ConcatAdapter(marketLoadingAdapter, marketItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketListViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketListViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })

        val marketCategoriesAdapter = MarketCategoriesAdapter(requireContext(), tabLayout, marketDiscoveryViewModel.marketCategories, this)
        marketCategoriesAdapter.selectCategory(marketDiscoveryViewModel.marketCategory)

        marketViewModel.discoveryListTypeLiveEvent.observe(viewLifecycleOwner) {
            marketListViewModel.updateSorting(it.sortingField)
            marketCategoriesAdapter.selectCategory(null)
        }

        marketListViewModel.topMenuLiveData.observe(viewLifecycleOwner) { (sortMenu, toggleButton) ->
            marketListHeader.setMenu(sortMenu, toggleButton)
        }
    }

    override fun onSortingClick() {
        val items = marketListViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketListViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketListViewModel.sortingFields[position]
                    marketListViewModel.updateSorting(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onToggleButtonClick() {
        marketListViewModel.onToggleButtonClick()
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinType, marketViewItem.coinUid, marketViewItem.coinCode, marketViewItem.coinName)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    override fun onSelect(marketCategory: MarketCategory?) {
        marketDiscoveryViewModel.marketCategory = marketCategory
    }
}
