package io.horizontalsystems.bankwallet.modules.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingModule
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingViewModel
import io.horizontalsystems.bankwallet.modules.main.MainActivity.Companion.ACTIVE_TAB_KEY
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceActivity
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

        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_market -> viewPager.setCurrentItem(0, false)
                R.id.navigation_balance -> viewPager.setCurrentItem(1, false)
                R.id.navigation_transactions -> viewPager.setCurrentItem(2, false)
                R.id.navigation_settings -> viewPager.setCurrentItem(3, false)
            }
            true
        }

        arguments?.getInt(ACTIVE_TAB_KEY)?.let { position ->
            bottomNavigation.menu.getItem(position).isChecked = true
            viewPager.setCurrentItem(position, false)
        }

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

        activity?.onBackPressedDispatcher?.addCallback(this) {
            if (findNavController().currentDestination?.id == R.id.mainFragment) {
                when (bottomNavigation.selectedItemId) {
                    R.id.navigation_market -> activity?.finish()
                    else -> bottomNavigation.selectedItemId = R.id.navigation_market
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onDestroyView() {
        viewPager.adapter = null
        bottomBadgeView = null

        super.onDestroyView()
    }

    //  RateAppDialogFragment.Listener

    override fun onClickRateApp() {
        openAppInPlayMarket()
    }

    private fun openAppInPlayMarket() {
        context?.let { context ->
            try {
                ContextCompat.startActivity(context, RateAppManager.getPlayMarketAppIntent(), null)
            } catch (e: ActivityNotFoundException) {
                ContextCompat.startActivity(context, RateAppManager.getPlayMarketSiteIntent(), null)
            }
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
