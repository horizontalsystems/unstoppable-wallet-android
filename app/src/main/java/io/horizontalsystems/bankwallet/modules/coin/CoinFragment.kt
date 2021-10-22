package io.horizontalsystems.bankwallet.modules.coin

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewFragment
import io.horizontalsystems.bankwallet.modules.coin.ui.CoinScreenTitle
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_coin.*

class CoinFragment : BaseFragment(R.layout.fragment_coin) {
    private val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) {
        CoinModule.Factory(requireArguments().getString(COIN_UID_KEY)!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager.adapter =
            CoinTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        viewModel.selectedTab.observe(viewLifecycleOwner) { selectedTab ->
            viewPager.setCurrentItem(viewModel.tabs.indexOf(selectedTab), false)
        }

        tabsCompose.setContent {
            ComposeAppTheme {
                Column {
                    CoinScreenTitle(viewModel.fullCoin.coin.name, viewModel.fullCoin.coin.marketCapRank, viewModel.fullCoin.coin.iconUrl)

                    val selectedTab by viewModel.selectedTab.observeAsState()
                    val tabItems = viewModel.tabs.map {
                        TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
                    }

                    Tabs(tabItems, onClick = {
                        viewModel.onSelect(it)
                    })
                }
            }
        }

        viewModel.titleLiveData.observe(viewLifecycleOwner) {
            toolbar.title = it
        }

        viewModel.isFavoriteLiveData.observe(viewLifecycleOwner) { isFavorite ->
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !isFavorite
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = isFavorite
        }

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuFavorite -> {
                    viewModel.onFavoriteClick()
                    HudHelper.showSuccessMessage(requireView(),
                        getString(R.string.Hud_Added_To_Watchlist))
                    true
                }
                R.id.menuUnfavorite -> {
                    viewModel.onUnfavoriteClick()
                    HudHelper.showSuccessMessage(requireView(),
                        getString(R.string.Hud_Removed_from_Watchlist))
                    true
                }
                else -> false
            }
        }

        tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}

class CoinTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CoinOverviewFragment()
            1 -> CoinMarketsFragment()
            else -> throw IllegalStateException()
        }
    }
}
