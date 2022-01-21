package io.horizontalsystems.bankwallet.modules.coin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.navGraphViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentCoinBinding
import io.horizontalsystems.bankwallet.modules.coin.coinmarkets.CoinMarketsFragment
import io.horizontalsystems.bankwallet.modules.coin.details.CoinDetailsFragment
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewFragment
import io.horizontalsystems.bankwallet.modules.coin.tweets.CoinTweetsFragment
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorMultipleDialog
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class CoinFragment : BaseFragment() {
    private val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) {
        CoinModule.Factory(requireArguments().getString(COIN_UID_KEY)!!)
    }

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val manageWalletsViewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val coinSettingsViewModel by viewModels<CoinSettingsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }
    private val coinPlatformsViewModel by viewModels<CoinPlatformsViewModel> { vmFactory }

    private var _binding: FragmentCoinBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewPager.adapter =
            CoinTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        binding.viewPager.isUserInputEnabled = false

        viewModel.selectedTab.observe(viewLifecycleOwner) { selectedTab ->
            binding.viewPager.setCurrentItem(viewModel.tabs.indexOf(selectedTab), false)
        }

        binding.tabsCompose.setContent {
            ComposeAppTheme {
                Column {
                    val selectedTab by viewModel.selectedTab.observeAsState()
                    val tabItems = viewModel.tabs.map {
                        TabItem(stringResource(id = it.titleResId), it == selectedTab, it)
                    }

                    ScrollableTabs(tabItems, onClick = {
                        viewModel.onSelect(it)
                    })
                }
            }
        }

        viewModel.titleLiveData.observe(viewLifecycleOwner) {
            binding.toolbar.title = it
        }

        viewModel.isFavoriteLiveData.observe(viewLifecycleOwner) { isFavorite ->
            binding.toolbar.menu.findItem(R.id.menuFavorite).isVisible = !isFavorite
            binding.toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = isFavorite
        }

        viewModel.coinStateLiveData.observe(viewLifecycleOwner) { coinState ->
            val menuAddToWallet: Boolean
            val menuInWallet: Boolean
            when (coinState) {
                null,
                CoinState.Unsupported,
                CoinState.NoActiveAccount,
                CoinState.WatchAccount -> {
                    menuAddToWallet = false
                    menuInWallet = false
                }
                CoinState.AddedToWallet,
                CoinState.InWallet -> {
                    menuAddToWallet = false
                    menuInWallet = true
                }
                CoinState.NotInWallet -> {
                    menuAddToWallet = true
                    menuInWallet = false
                }
            }

            binding.toolbar.menu.findItem(R.id.menuAddToWallet).isVisible = menuAddToWallet
            binding.toolbar.menu.findItem(R.id.menuInWallet).isVisible = menuInWallet
        }

        viewModel.warningMessageLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showInProcessMessage(requireView(), it, showProgressBar = false)
        }

        viewModel.successMessageLiveEvent.observe(viewLifecycleOwner) {
            HudHelper.showSuccessMessage(requireView(), it)
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuFavorite -> {
                    viewModel.onFavoriteClick()
                    true
                }
                R.id.menuAddToWallet -> {
                    manageWalletsViewModel.enable(viewModel.fullCoin.coin.uid)
                    true
                }
                R.id.menuInWallet -> {
                    viewModel.onClickInWallet()
                    true
                }
                R.id.menuUnfavorite -> {
                    viewModel.onUnfavoriteClick()
                    true
                }
                else -> false
            }
        }

        binding.tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        observe()
    }

    private fun observe() {
        coinSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner) { config ->
            hideKeyboard()
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinSettingsViewModel.onSelect(indexes) },
                onCancel = { coinSettingsViewModel.onCancelSelect() }
            )
        }

        restoreSettingsViewModel.openBirthdayAlertSignal.observe(viewLifecycleOwner) {
            val zcashBirhdayHeightDialog = ZcashBirthdayHeightDialog()
            zcashBirhdayHeightDialog.onEnter = {
                restoreSettingsViewModel.onEnter(it)
            }
            zcashBirhdayHeightDialog.onCancel = {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }

            zcashBirhdayHeightDialog.show(
                requireActivity().supportFragmentManager,
                "ZcashBirthdayHeightDialog"
            )
        }

        coinPlatformsViewModel.openPlatformsSelectorEvent.observe(viewLifecycleOwner) { config ->
            showBottomSelectorDialog(
                config,
                onSelect = { indexes -> coinPlatformsViewModel.onSelect(indexes) },
                onCancel = { coinPlatformsViewModel.onCancelSelect() }
            )
        }
    }

    private fun showBottomSelectorDialog(
        config: BottomSheetSelectorMultipleDialog.Config,
        onSelect: (indexes: List<Int>) -> Unit,
        onCancel: () -> Unit
    ) {
        BottomSheetSelectorMultipleDialog.show(
            fragmentManager = childFragmentManager,
            title = config.title,
            subtitle = config.subtitle,
            icon = config.icon,
            items = config.viewItems,
            selected = config.selectedIndexes,
            notifyUnchanged = true,
            onItemSelected = { onSelect(it) },
            onCancelled = { onCancel() },
            warning = config.description
        )
    }


    companion object {
        private const val COIN_UID_KEY = "coin_uid_key"

        fun prepareParams(coinUid: String) = bundleOf(COIN_UID_KEY to coinUid)
    }
}

class CoinTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CoinOverviewFragment()
            1 -> CoinMarketsFragment()
            2 -> CoinDetailsFragment()
            3 -> CoinTweetsFragment()
            else -> throw IllegalStateException()
        }
    }
}
