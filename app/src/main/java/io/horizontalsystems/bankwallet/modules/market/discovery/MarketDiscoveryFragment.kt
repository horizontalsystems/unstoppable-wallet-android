package io.horizontalsystems.bankwallet.modules.market.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.MarketViewModel
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketTopViewItem
import io.horizontalsystems.bankwallet.modules.market.top.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market_discovery.*

class MarketDiscoveryFragment : BaseFragment(), MarketListHeaderView.Listener, ViewHolderMarketTopItem.Listener, MarketCategoriesAdapter.Listener {

    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter
    private lateinit var marketCategoriesAdapter: MarketCategoriesAdapter

    private val marketDiscoveryViewModel by viewModels<MarketDiscoveryViewModel> { MarketDiscoveryModule.Factory() }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_discovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this
        marketListHeader.setSortingField(marketDiscoveryViewModel.sortingField)
        marketListHeader.setMarketField(marketDiscoveryViewModel.marketField)
        marketListHeader.isVisible = false
        marketDiscoveryViewModel.marketTopViewItemsLiveData.observe(viewLifecycleOwner, {
            marketListHeader.isVisible = it.isNotEmpty()
        })

        marketTopItemsAdapter = MarketTopItemsAdapter(
                this,
                marketDiscoveryViewModel.marketTopViewItemsLiveData,
                marketDiscoveryViewModel.loadingLiveData,
                marketDiscoveryViewModel.errorLiveData,
                viewLifecycleOwner
        )
        marketLoadingAdapter = MarketLoadingAdapter(marketDiscoveryViewModel.loadingLiveData, marketDiscoveryViewModel.errorLiveData, marketDiscoveryViewModel::onErrorClick, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketLoadingAdapter, marketTopItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketDiscoveryViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketDiscoveryViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })

        marketViewModel.discoveryListTypeLiveEvent.observe(viewLifecycleOwner) {
            marketListHeader.setSortingField(it.sortingField)
            marketListHeader.setMarketField(it.marketField)

            marketDiscoveryViewModel.update(sortingField = it.sortingField, marketField = it.marketField)

            marketCategoriesAdapter.selectCategory(null)
        }

        marketCategoriesAdapter = MarketCategoriesAdapter(requireContext(), tabLayout, marketDiscoveryViewModel.marketCategories, this)
    }

    override fun onClickSortingField() {
        val items = marketDiscoveryViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketDiscoveryViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketDiscoveryViewModel.sortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    marketDiscoveryViewModel.update(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectMarketField(marketField: MarketField) {
        marketDiscoveryViewModel.update(marketField = marketField)
    }

    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }

    override fun onSelect(marketCategory: MarketCategory?) {
        marketDiscoveryViewModel.onSelectMarketCategory(marketCategory)
    }
}
