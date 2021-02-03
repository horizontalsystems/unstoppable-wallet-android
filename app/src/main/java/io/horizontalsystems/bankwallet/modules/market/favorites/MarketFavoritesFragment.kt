package io.horizontalsystems.bankwallet.modules.market.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_favorites.*

class MarketFavoritesFragment : BaseFragment(), MarketListHeaderView.Listener, ViewHolderMarketItem.Listener {

    private lateinit var marketItemsAdapter: MarketItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter

    private val marketFavoritesViewModel by viewModels<MarketFavoritesViewModel> { MarketFavoritesModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this
        marketListHeader.setSortingField(marketFavoritesViewModel.sortingField)
        marketListHeader.setMarketField(marketFavoritesViewModel.marketField)
        marketListHeader.isVisible = false
        marketFavoritesViewModel.marketViewItemsLiveData.observe(viewLifecycleOwner, { (list, _) ->
            marketListHeader.isVisible = list.isNotEmpty()
        })

        marketItemsAdapter = MarketItemsAdapter(
                this,
                marketFavoritesViewModel.marketViewItemsLiveData,
                marketFavoritesViewModel.loadingLiveData,
                marketFavoritesViewModel.errorLiveData,
                viewLifecycleOwner
        )
        marketLoadingAdapter = MarketLoadingAdapter(marketFavoritesViewModel.loadingLiveData, marketFavoritesViewModel.errorLiveData, marketFavoritesViewModel::onErrorClick, viewLifecycleOwner)

        val emptyListAdapter = EmptyListAdapter(marketFavoritesViewModel.showEmptyListTextLiveData, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketLoadingAdapter, marketItemsAdapter, emptyListAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketFavoritesViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketFavoritesViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })
    }

    override fun onClickSortingField() {
        val items = marketFavoritesViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketFavoritesViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketFavoritesViewModel.sortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    marketFavoritesViewModel.update(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectMarketField(marketField: MarketField) {
        marketFavoritesViewModel.update(marketField = marketField)
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = RateChartFragment.prepareParams(marketViewItem.coinCode, marketViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
