package io.horizontalsystems.bankwallet.modules.coin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewFragment
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
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

    private var notificationMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager.adapter =
            CoinTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        notificationMenuItem = toolbar.menu.findItem(R.id.menuNotification)
        updateNotificationMenuItem()

        viewModel.selectedTab.observe(viewLifecycleOwner) { selectedTab ->
            setTabs(selectedTab)
            viewPager.setCurrentItem(viewModel.tabs.indexOf(selectedTab), false)
        }

        viewModel.titleLiveData.observe(viewLifecycleOwner) {
            toolbar.title = it
        }

        viewModel.isFavoriteLiveData.observe(viewLifecycleOwner) { isFavorite ->
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !isFavorite
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = isFavorite
        }

        viewModel.alertNotificationUpdated.observe(viewLifecycleOwner) {
            updateNotificationMenuItem()
        }

        viewModel.showNotificationMenu.observe(viewLifecycleOwner, Observer { (coinType, coinName) ->
            BottomNotificationMenu.show(childFragmentManager, NotificationMenuMode.All, coinName, coinType)
        })

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
                R.id.menuNotification -> {
                    viewModel.onNotificationClick()
                    true
                }
                else -> false
            }
        }

        tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setTabs(selectedTab: CoinModule.Tab) {
        val tabItems = viewModel.tabs.map {
            TabItem(getString(it.titleResId), it == selectedTab, it)
        }
        tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabItems) { item ->
                    viewModel.onSelect(item)
                }
            }
        }
    }

    private fun updateNotificationMenuItem() {
        notificationMenuItem?.apply {
            isVisible = viewModel.notificationIconVisible
            icon = context?.let {
                val iconRes = if (viewModel.notificationIconActive) R.drawable.ic_notification_24 else R.drawable.ic_notification_disabled
                ContextCompat.getDrawable(it, iconRes)
            }
        }
    }

    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}

class CoinTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 1

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CoinOverviewFragment()
            else -> throw IllegalStateException()
        }
    }
}
