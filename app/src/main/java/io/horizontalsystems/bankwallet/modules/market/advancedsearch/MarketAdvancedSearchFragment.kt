package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.selector.ItemViewHolder
import io.horizontalsystems.bankwallet.ui.selector.ItemViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.SelectorBottomSheetDialog
import io.horizontalsystems.bankwallet.ui.selector.SelectorItemViewHolderFactory
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_search_filter.*

class MarketAdvancedSearchFragment : BaseFragment() {

    private val marketAdvancedSearchViewModel by navGraphViewModels<MarketAdvancedSearchViewModel>(R.id.marketAdvancedSearchFragment) {
        MarketAdvancedSearchModule.Factory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_search_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuReset -> {
                    marketAdvancedSearchViewModel.reset()
                    true
                }
                else -> false
            }
        }

        marketAdvancedSearchViewModel.coinListViewItemLiveData.observe(viewLifecycleOwner) {
            filterCoinList.setValueColored(it.title, it.color)
        }
        filterCoinList.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_ChooseSet,
                    subtitleText = "---------",
                    headerIcon = R.drawable.ic_circle_coin_24,
                    items = marketAdvancedSearchViewModel.coinListsViewItemOptions,
                    selectedItem = marketAdvancedSearchViewModel.coinListViewItem,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketAdvancedSearchViewModel.coinListViewItem = it
            }
        }

        marketAdvancedSearchViewModel.marketCapViewItemLiveData.observe(viewLifecycleOwner) {
            filterMarketCap.setValueColored(it.title, it.color)
        }
        filterMarketCap.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_MarketCap,
                    subtitleText = "---------",
                    headerIcon = R.drawable.ic_usd_24,
                    items = marketAdvancedSearchViewModel.marketCapViewItemOptions,
                    selectedItem = marketAdvancedSearchViewModel.marketCapViewItem,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketAdvancedSearchViewModel.marketCapViewItem = it
            }
        }

        marketAdvancedSearchViewModel.volumeViewItemLiveData.observe(viewLifecycleOwner) {
            filterVolume.setValueColored(it.title, it.color)
        }
        filterVolume.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_Volume,
                    subtitleText = getString(R.string.TimePeriod_24h),
                    headerIcon = R.drawable.ic_chart_24,
                    items = marketAdvancedSearchViewModel.volumeViewItemOptions,
                    selectedItem = marketAdvancedSearchViewModel.volumeViewItem,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketAdvancedSearchViewModel.volumeViewItem = it
            }
        }

        marketAdvancedSearchViewModel.periodViewItemLiveData.observe(viewLifecycleOwner) {
            filterPeriod.setValueColored(it.title, it.color)
        }
        filterPeriod.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_PricePeriod,
                    subtitleText = "---------",
                    headerIcon = R.drawable.ic_circle_clock_24,
                    items = marketAdvancedSearchViewModel.periodViewItemOptions,
                    selectedItem = marketAdvancedSearchViewModel.periodViewItem,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketAdvancedSearchViewModel.periodViewItem = it
            }
        }

        marketAdvancedSearchViewModel.priceChangeViewItemLiveData.observe(viewLifecycleOwner) {
            filterPriceChange.setValueColored(it.title, it.color)
        }
        filterPriceChange.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_PriceChange,
                    subtitleText = "---------",
                    headerIcon = R.drawable.ic_market_24,
                    items = marketAdvancedSearchViewModel.priceChangeViewItemOptions,
                    selectedItem = marketAdvancedSearchViewModel.priceChangeViewItem,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketAdvancedSearchViewModel.priceChangeViewItem = it
            }
        }

        submit.setOnSingleClickListener {
            findNavController().navigate(R.id.marketAdvancedSearchFragment_to_marketAdvancedSearchFragmentResults, null, navOptions())
        }

        marketAdvancedSearchViewModel.showResultsTitleLiveData.observe(viewLifecycleOwner) {
            submit.text = it
        }

        marketAdvancedSearchViewModel.showResultsEnabledLiveData.observe(viewLifecycleOwner) {
            submit.isEnabled = it
        }

        marketAdvancedSearchViewModel.errorLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        marketAdvancedSearchViewModel.loadingLiveData.observe(viewLifecycleOwner) {
            progressBar.isVisible = it
        }
    }

    private fun <ItemClass> showSelectorDialog(title: Int, subtitleText: String, headerIcon: Int, items: List<ItemClass>, selectedItem: ItemClass, itemViewHolderFactory: ItemViewHolderFactory<ItemViewHolder<ItemClass>>, onSelectListener: (ItemClass) -> Unit) {
        val dialog = SelectorBottomSheetDialog<ItemClass>()
        dialog.titleText = getString(title)
        dialog.subtitleText = subtitleText
        dialog.headerIconResourceId = headerIcon
        dialog.items = items
        dialog.selectedItem = selectedItem
        dialog.onSelectListener = onSelectListener
        dialog.itemViewHolderFactory = itemViewHolderFactory

        dialog.show(childFragmentManager, "selector_dialog")
    }
}


