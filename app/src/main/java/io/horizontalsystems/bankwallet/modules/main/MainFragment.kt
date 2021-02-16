package io.horizontalsystems.bankwallet.modules.main

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.modules.main.MainActivity.Companion.ACTIVE_TAB_KEY
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*

class MainFragment : Fragment(), RateAppDialogFragment.Listener {

    private val viewModel by viewModels<MainViewModel>()
    private var bottomBadgeView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        view.viewPager.offscreenPageLimit = 1
        view.viewPager.adapter = MainViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        view.viewPager.isUserInputEnabled = false

        view.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_market -> view.viewPager.setCurrentItem(0, false)
                R.id.navigation_balance -> view.viewPager.setCurrentItem(1, false)
                R.id.navigation_transactions -> view.viewPager.setCurrentItem(2, false)
                R.id.navigation_settings -> view.viewPager.setCurrentItem(3, false)
            }
            true
        }

        arguments?.getInt(ACTIVE_TAB_KEY)?.let { position ->
            view.bottomNavigation.menu.getItem(position).isChecked = true
            view.viewPager.setCurrentItem(position, false)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.init()
        viewModel.showRateAppLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                RateAppDialogFragment.show(it, this)
            }
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
            when  {
                findNavController().currentDestination?.id == R.id.mainFragment && viewPager.currentItem == 0 -> activity?.finish()
                findNavController().currentDestination?.id == R.id.mainFragment  -> viewPager.setCurrentItem(0, false)
                else -> findNavController().popBackStack()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.delegate.onResume()
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
