package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingModule
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingViewModel
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceActivity
import io.horizontalsystems.bankwallet.ui.selector.SelectorBottomSheetDialog
import io.horizontalsystems.bankwallet.ui.selector.SelectorRadioItemViewHolderFactory
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : BaseFragment(R.layout.fragment_main), RateAppDialogFragment.Listener {

    private val viewModel by viewModels<MainViewModel>{ MainModule.Factory() }
    private val balanceOnboardingViewModel by viewModels<BalanceOnboardingViewModel> { BalanceOnboardingModule.Factory() }
    private var bottomBadgeView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainViewPagerAdapter = MainViewPagerAdapter(this)

        viewPager.offscreenPageLimit = 1
        viewPager.adapter = mainViewPagerAdapter
        viewPager.isUserInputEnabled = false

        viewPager.setCurrentItem(viewModel.initialTab.ordinal, false)
        bottomNavigation.menu.getItem(viewModel.initialTab.ordinal).isChecked = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                viewModel.onTabSelect(position)
            }
        })

        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_market -> viewPager.setCurrentItem(0, false)
                R.id.navigation_balance -> viewPager.setCurrentItem(1, false)
                R.id.navigation_transactions -> viewPager.setCurrentItem(2, false)
                R.id.navigation_settings -> viewPager.setCurrentItem(3, false)
            }
            true
        }

        bottomNavigation.findViewById<View>(R.id.navigation_balance)?.setOnLongClickListener {
            viewModel.onLongPressBalanceTab()
            true
        }

        viewModel.openWalletSwitcherLiveEvent.observe(viewLifecycleOwner, { (wallets, selectedWallet) ->
            openWalletSwitchDialog(wallets, selectedWallet) {
                viewModel.onSelect(it)
            }
        })

        balanceOnboardingViewModel.balanceViewTypeLiveData.observe(viewLifecycleOwner) {
            mainViewPagerAdapter.balancePageType = it
        }

        viewModel.showRootedDeviceWarningLiveEvent.observe(viewLifecycleOwner, {
            startActivity(Intent(activity, RootedDeviceActivity::class.java))
        })

        viewModel.showRateAppLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                RateAppDialogFragment.show(it, this)
            }
        })

        viewModel.showWhatsNewLiveEvent.observe(viewLifecycleOwner, {
            findNavController().navigate(
                    R.id.mainFragment_to_releaseNotesFragment,
                    bundleOf(ReleaseNotesFragment.showAsClosablePopupKey to true),
                    navOptionsFromBottom()
            )
        })

        viewModel.openPlayMarketLiveEvent.observe(viewLifecycleOwner, Observer {
            openAppInPlayMarket()
        })

        viewModel.hideContentLiveData.observe(viewLifecycleOwner, Observer { hide ->
            screenSecureDim.isVisible = hide
        })

        viewModel.setBadgeVisibleLiveData.observe(viewLifecycleOwner, Observer { visible ->
            val bottomMenu = bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
            val settingsNavigationViewItem = bottomMenu?.getChildAt(3) as? BottomNavigationItemView

            if (visible) {
                if (bottomBadgeView?.parent == null) {
                    settingsNavigationViewItem?.addView(getBottomBadge())
                }
            } else {
                settingsNavigationViewItem?.removeView(bottomBadgeView)
            }
        })

        viewModel.transactionTabEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            bottomNavigation.menu.getItem(2).isEnabled = enabled
        })

    }

    private fun openWalletSwitchDialog(
        items: List<ViewItemWrapper<Account>>,
        selectedItem: ViewItemWrapper<Account>?,
        onSelectListener: (account: Account) -> Unit
    ) {
        val dialog = SelectorBottomSheetDialog<ViewItemWrapper<Account>>()
        dialog.titleText = getString(R.string.ManageAccount_SwitchWallet_Title)
        dialog.subtitleText = getString(R.string.ManageAccount_SwitchWallet_Subtitle)
        dialog.headerIconResourceId = R.drawable.ic_switch_wallet
        dialog.items = items
        dialog.selectedItem = selectedItem
        dialog.onSelectListener = { onSelectListener(it.item) }
        dialog.itemViewHolderFactory = SelectorRadioItemViewHolderFactory()

        dialog.show(childFragmentManager, "selector_dialog")
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        bottomBadgeView = null

        super.onDestroyView()
    }

    //  RateAppDialogFragment.Listener

    override fun onClickRateApp() {
        openAppInPlayMarket()
    }

    private fun openAppInPlayMarket() {
        context?.let { context ->
            RateAppManager.openPlayMarket(context)
        }
    }

    private fun getBottomBadge(): View? {
        if (bottomBadgeView != null) {
            return bottomBadgeView
        }

        val bottomMenu = bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
        bottomBadgeView = LayoutInflater.from(activity).inflate(R.layout.view_bottom_navigation_badge, bottomMenu, false)

        return bottomBadgeView
    }
}
