package io.horizontalsystems.bankwallet.modules.market.discovery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.MarketInternalNavigationViewModel
import io.horizontalsystems.bankwallet.modules.market.top.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market_discovery.*

class MarketDiscoveryFragment : BaseFragment(), MarketTopHeaderAdapter.Listener, ViewHolderMarketTopItem.Listener, MarketCategoriesAdapter.Listener {

    private lateinit var marketTopHeaderAdapter: MarketTopHeaderAdapter
    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter
    private lateinit var marketCategoriesAdapter: MarketCategoriesAdapter

    enum class Mode {
        TopGainers, TopLosers, TopByVolume
    }

    private val marketTopViewModel by viewModels<MarketTopViewModel> { MarketTopModule.Factory() }
    private val navigationViewModel by navGraphViewModels<MarketInternalNavigationViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_discovery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketTopHeaderAdapter = MarketTopHeaderAdapter(this, marketTopViewModel.sortingField, marketTopViewModel.marketField)
        marketTopItemsAdapter = MarketTopItemsAdapter(
                this,
                marketTopViewModel.marketTopViewItemsLiveData,
                marketTopViewModel.loadingLiveData,
                marketTopViewModel.errorLiveData,
                viewLifecycleOwner
        )
        marketLoadingAdapter = MarketLoadingAdapter(marketTopViewModel, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketTopHeaderAdapter, marketLoadingAdapter, marketTopItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketTopViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketTopViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })

        navigationViewModel.discoveryModeLiveEvent.observe(viewLifecycleOwner) {
            when (it) {
                Mode.TopGainers -> {
                    marketTopHeaderAdapter.update(sortingField = Field.HighestCap, marketField = MarketField.MarketCap)
                    marketTopViewModel.update(sortingField = Field.HighestCap, marketField = MarketField.MarketCap)
                }
                Mode.TopLosers -> {
                    marketTopHeaderAdapter.update(sortingField = Field.LowestCap, marketField = MarketField.MarketCap)
                    marketTopViewModel.update(sortingField = Field.LowestCap, marketField = MarketField.MarketCap)
                }
                Mode.TopByVolume -> {
                    marketTopHeaderAdapter.update(sortingField = Field.HighestVolume, marketField = MarketField.Volume)
                    marketTopViewModel.update(sortingField = Field.HighestVolume, marketField = MarketField.Volume)
                }
                else -> Unit
            }
            marketCategoriesAdapter.selectCategory(null)
        }

        marketCategoriesAdapter = MarketCategoriesAdapter(requireContext(), tabLayout, marketTopViewModel.marketCategories, this)
    }

    override fun onClickSortingField() {
        val items = marketTopViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketTopViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketTopViewModel.sortingFields[position]

                    marketTopHeaderAdapter.update(sortingField = selectedSortingField)
                    marketTopViewModel.update(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectMarketField(marketField: MarketField) {
        marketTopViewModel.update(marketField = marketField)
    }

    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }

    override fun onSelect(marketCategory: MarketCategory) {

    }
}
