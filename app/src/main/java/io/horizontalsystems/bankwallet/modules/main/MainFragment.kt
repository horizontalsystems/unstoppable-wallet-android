package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.databinding.FragmentMainBinding
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingModule
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingViewModel
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceActivity
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetWalletSelectDialog
import io.horizontalsystems.core.findNavController

class MainFragment : BaseFragment(), RateAppDialogFragment.Listener {

    private val viewModel by viewModels<MainViewModel> { MainModule.Factory() }
    private val balanceOnboardingViewModel by viewModels<BalanceOnboardingViewModel> { BalanceOnboardingModule.Factory() }
    private var bottomBadgeView: View? = null

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomBadgeView = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainViewPagerAdapter = MainViewPagerAdapter(this)

        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = mainViewPagerAdapter
        binding.viewPager.isUserInputEnabled = false

        binding.viewPager.setCurrentItem(viewModel.initialTab.ordinal, false)
        binding.bottomNavigation.menu.getItem(viewModel.initialTab.ordinal).isChecked = true

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onTabSelect(position)
            }
        })

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_market -> binding.viewPager.setCurrentItem(0, false)
                R.id.navigation_balance -> binding.viewPager.setCurrentItem(1, false)
                R.id.navigation_transactions -> binding.viewPager.setCurrentItem(2, false)
                R.id.navigation_settings -> binding.viewPager.setCurrentItem(3, false)
            }
            true
        }

        binding.bottomNavigation.findViewById<View>(R.id.navigation_balance)
            ?.setOnLongClickListener {
                viewModel.onLongPressBalanceTab()
                true
            }

        viewModel.openWalletSwitcherLiveEvent.observe(
            viewLifecycleOwner,
            { (wallets, selectedWallet) ->
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
            findNavController().slideFromBottom(
                R.id.mainFragment_to_releaseNotesFragment,
                bundleOf(ReleaseNotesFragment.showAsClosablePopupKey to true)
            )
        })

        viewModel.openPlayMarketLiveEvent.observe(viewLifecycleOwner, Observer {
            openAppInPlayMarket()
        })

        viewModel.hideContentLiveData.observe(viewLifecycleOwner, Observer { hide ->
            binding.screenSecureDim.isVisible = hide
        })

        viewModel.setBadgeVisibleLiveData.observe(viewLifecycleOwner, Observer { visible ->
            val bottomMenu = binding.bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
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
            binding.bottomNavigation.menu.getItem(2).isEnabled = enabled
        })

    }

    private fun openWalletSwitchDialog(
        items: List<Account>,
        selectedItem: Account?,
        onSelectListener: (account: Account) -> Unit
    ) {
        val dialog = BottomSheetWalletSelectDialog()
        dialog.items = items
        dialog.selectedItem = selectedItem
        dialog.onSelectListener = { onSelectListener(it) }

        dialog.show(childFragmentManager, "selector_dialog")
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
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

        val bottomMenu = binding.bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
        bottomBadgeView = LayoutInflater.from(activity)
            .inflate(R.layout.view_bottom_navigation_badge, bottomMenu, false)

        return bottomBadgeView
    }
}
