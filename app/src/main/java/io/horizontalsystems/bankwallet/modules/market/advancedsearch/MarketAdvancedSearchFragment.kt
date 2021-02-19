package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.selector.*
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_market_search_filter.*

class MarketAdvancedSearchFragment : BaseFragment() {

    private val marketSearchFilterViewModel by viewModels<MarketAdvancedSearchViewModel> { MarketAdvancedSearchModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_search_filter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        filterCoinList.setValue(marketSearchFilterViewModel.coinList.title)
        filterCoinList.setValueColor(marketSearchFilterViewModel.coinList.color)
        filterCoinList.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_CoinList,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_circle_coin_24,
                    items = marketSearchFilterViewModel.coinLists,
                    selectedItem = marketSearchFilterViewModel.coinList,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.coinList = it
                filterCoinList.setValue(it.title)
                filterCoinList.setValueColor(it.color)
            }
        }

        filterMarketCap.setValue(marketSearchFilterViewModel.marketCap.title)
        filterMarketCap.setValueColor(marketSearchFilterViewModel.marketCap.color)
        filterMarketCap.setOnSingleClickListener {
            showSelectorDialog(
                    title = R.string.Market_Filter_MarketCap,
                    subtitleText = "Range",
                    headerIcon = R.drawable.ic_circle_coin_24,
                    items = marketSearchFilterViewModel.marketCaps,
                    selectedItem = marketSearchFilterViewModel.marketCap,
                    itemViewHolderFactory = SelectorItemViewHolderFactory()
            ) {
                marketSearchFilterViewModel.marketCap = it
                filterMarketCap.setValue(it.title)
                filterMarketCap.setValueColor(it.color)
            }
        }

        submit.setOnSingleClickListener {
            marketSearchFilterViewModel.showResults()
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


