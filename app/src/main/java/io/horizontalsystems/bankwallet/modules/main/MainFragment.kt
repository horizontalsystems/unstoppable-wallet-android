package io.horizontalsystems.bankwallet.modules.main

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
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
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentMainBinding
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesFragment
import io.horizontalsystems.bankwallet.modules.rooteddevice.RootedDeviceActivity
import io.horizontalsystems.bankwallet.modules.walletconnect.WCAccountTypeNotSupportedDialog
import io.horizontalsystems.bankwallet.modules.walletconnect.version1.WC1Manager.SupportState
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetWalletSelectDialog
import io.horizontalsystems.core.findNavController

class MainFragment : BaseFragment(), RateAppDialogFragment.Listener {

    private val viewModel by viewModels<MainViewModel> {
        MainModule.Factory(activity?.intent?.data?.toString())
    }
    private var bottomBadgeView: View? = null

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().moveTaskToBack(true)
            }
        })
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

        binding.bottomNavigation.setOnItemSelectedListener {
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

        viewModel.walletConnectSupportState.observe(viewLifecycleOwner) { wcSupportState ->
            viewModel.wcSupportStateHandled()
            when (wcSupportState) {
                SupportState.Supported -> {
                    findNavController().slideFromRight(R.id.wallet_connect_graph)
                }
                SupportState.NotSupportedDueToNoActiveAccount -> {
                    activity?.intent?.data = null
                    findNavController().slideFromBottom(R.id.wcErrorNoAccountFragment)
                }
                is SupportState.NotSupported -> {
                    activity?.intent?.data = null
                    findNavController().slideFromBottom(
                        R.id.wcAccountTypeNotSupportedDialog,
                        WCAccountTypeNotSupportedDialog.prepareParams(wcSupportState.accountTypeDescription)
                    )
                }
                null -> {}
            }
        }


        viewModel.openWalletSwitcherLiveEvent.observe(
            viewLifecycleOwner,
            { (wallets, selectedWallet) ->
                openWalletSwitchDialog(wallets, selectedWallet) {
                    viewModel.onSelect(it)
                }
            })

        viewModel.showRootedDeviceWarningLiveEvent.observe(viewLifecycleOwner, {
            startActivity(Intent(activity, RootedDeviceActivity::class.java))
        })

        viewModel.showRateAppLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                RateAppDialogFragment.show(it, this)
            }
        })

        viewModel.showWhatsNewLiveEvent.observe(viewLifecycleOwner) {
            findNavController().slideFromBottom(
                R.id.releaseNotesFragment,
                bundleOf(ReleaseNotesFragment.showAsClosablePopupKey to true)
            )
        }

        viewModel.openPlayMarketLiveEvent.observe(viewLifecycleOwner, Observer {
            openAppInPlayMarket()
        })

        viewModel.hideContentLiveData.observe(viewLifecycleOwner, Observer { hide ->
            binding.screenSecureDim.isVisible = hide
        })

        viewModel.settingsBadgeLiveData.observe(viewLifecycleOwner) {
            setSettingsBadge(it)
        }

        viewModel.transactionTabEnabledLiveData.observe(viewLifecycleOwner, { enabled ->
            binding.bottomNavigation.menu.getItem(2).isEnabled = enabled
        })

        viewModel.marketsTabEnabledLiveData.observe(viewLifecycleOwner) { enabled ->
            binding.bottomNavigation.menu.getItem(0).isVisible = enabled
        }

        viewModel.torIsActiveLiveData.observe(viewLifecycleOwner) { torIsActive ->
            binding.torIsActiveState.isVisible = torIsActive
        }
        viewModel.playTorActiveAnimationLiveData.observe(viewLifecycleOwner) { playAnimation ->
            if (playAnimation) {
                context?.let { ctx ->
                    ValueAnimator().apply {
                        setIntValues(
                            ContextCompat.getColor(ctx, R.color.remus),
                            ContextCompat.getColor(ctx, R.color.lawrence)
                        )
                        setEvaluator(ArgbEvaluator())
                        addUpdateListener { valueAnimator ->
                            binding.torIsActiveState.setBackgroundColor((valueAnimator.animatedValue as Int))
                        }

                        duration = 1000
                        start()
                    }
                }
                viewModel.animationPlayed()
            }
        }

    }

    private fun openWalletSwitchDialog(
        items: List<Account>,
        selectedItem: Account?,
        onSelectListener: (account: Account) -> Unit
    ) {
        val dialog = BottomSheetWalletSelectDialog()
        dialog.wallets = items.filter { !it.isWatchAccount }
        dialog.watchingAddresses = items.filter { it.isWatchAccount }
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

    private fun setSettingsBadge(badgeType: MainModule.BadgeType?) {
        val bottomMenu = binding.bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
        val settingsNavigationViewItem = bottomMenu?.getChildAt(3) as? BottomNavigationItemView
        settingsNavigationViewItem?.removeView(bottomBadgeView)
        when (val type = badgeType) {
            MainModule.BadgeType.BadgeDot -> {
                bottomBadgeView = LayoutInflater.from(activity)
                    .inflate(R.layout.view_bottom_navigation_badge, bottomMenu, false)
                settingsNavigationViewItem?.addView(bottomBadgeView)
            }
            is MainModule.BadgeType.BadgeNumber -> {
                bottomBadgeView = LayoutInflater.from(activity)
                    .inflate(R.layout.view_bottom_navigation_badge_with_number, bottomMenu, false)
                bottomBadgeView?.findViewById<TextView>(R.id.badgeNumber)?.text = type.number.toString()
                settingsNavigationViewItem?.addView(bottomBadgeView)
            }
            else -> { }
        }
    }

}
