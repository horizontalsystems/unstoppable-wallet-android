package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_coin_markets.*
import kotlinx.android.synthetic.main.fragment_coin_markets.marketListHeader

class CoinMarketsFragment : BaseFragment(), MarketListHeaderView.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_coin_markets, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.CoinMarket_Title, coinViewModel.coinCode)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        marketListHeader.listener = this
        marketListHeader.setSortingField(coinViewModel.coinMarketsSortingField)
        marketListHeader.setFieldViewOptions(coinViewModel.coinMarketFieldViewOptions)

        val marketItemsAdapter = CoinMarketItemAdapter()

        recyclerView.adapter = marketItemsAdapter
        recyclerView.itemAnimator = null

        coinViewModel.coinMarkets.observe(viewLifecycleOwner, {
            marketItemsAdapter.submitList(it)
        })
    }

    override fun onClickSortingField() {
        val items = coinViewModel.coinMarketsSortingFields.map {
            SelectorItem(getString(it.titleResId), it == coinViewModel.coinMarketsSortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = coinViewModel.coinMarketsSortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    coinViewModel.update(selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectFieldViewOption(fieldViewOptionId: Int) {
        coinViewModel.update(fieldViewOptionId = fieldViewOptionId)
    }
}
