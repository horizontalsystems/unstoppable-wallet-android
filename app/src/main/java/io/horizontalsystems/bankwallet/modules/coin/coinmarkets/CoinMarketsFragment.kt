package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import kotlinx.android.synthetic.main.fragment_coin_markets.*
import kotlinx.android.synthetic.main.fragment_coin_markets.recyclerView

class CoinMarketsFragment : BaseFragment(R.layout.fragment_coin_markets), MarketListHeaderView.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinMarketsViewModel>{
        CoinMarketsModule.Factory(coinViewModel.fullCoin)
    }

    private var scrollToTopAfterUpdate = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketListHeader.listener = this

        val marketItemsAdapter = CoinMarketItemAdapter()

        recyclerView.adapter = marketItemsAdapter
        recyclerView.itemAnimator = null

        viewModel.tickersLiveData.observe(viewLifecycleOwner, { items ->
            marketItemsAdapter.submitList(items) {
                if (scrollToTopAfterUpdate) {
                    recyclerView.scrollToPosition(0)
                    scrollToTopAfterUpdate = false
                }
            }
        })

        viewModel.topMenuLiveData.observe(viewLifecycleOwner, { (sortMenu, toggleButton) ->
            marketListHeader.setMenu(sortMenu, toggleButton)
        })
    }

    override fun onSortingClick() {
        scrollToTopAfterUpdate = true
        viewModel.onSwitchSortType()
    }

    override fun onToggleButtonClick() {
        viewModel.onSwitchVolumeType()
    }

}
