package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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
    private val viewModel by viewModels<CoinMarketsViewModel>{ CoinMarketsModule.Factory(coinViewModel.coinCode) }

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
        marketListHeader.setSortingField(viewModel.sortingField)
        marketListHeader.setFieldViewOptions(viewModel.fieldViewOptions)

        val marketItemsAdapter = CoinMarketItemAdapter()

        recyclerView.adapter = marketItemsAdapter
        recyclerView.itemAnimator = null

        coinViewModel.coinMarketTickers.observe(viewLifecycleOwner, {
            viewModel.marketTickers = it
        })

        viewModel.coinMarketItems.observe(viewLifecycleOwner, {
            marketItemsAdapter.submitList(it)
        })
    }

    override fun onClickSortingField() {
        val items = viewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == viewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = viewModel.sortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    viewModel.update(selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectFieldViewOption(fieldViewOptionId: Int) {
        viewModel.update(fieldViewOptionId = fieldViewOptionId)
    }
}
