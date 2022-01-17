package io.horizontalsystems.bankwallet.modules.managewallets

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.core.findNavController

class ManageWalletsFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.ManageCoins_title)

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val coinSettingsViewModel by viewModels<CoinSettingsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }
    private val coinPlatformsViewModel by viewModels<CoinPlatformsViewModel> { vmFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.manage_wallets_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddToken -> {
                    hideKeyboard()
                    findNavController().navigate(
                        R.id.manageWalletsFragment_to_addToken,
                        null,
                        navOptions()
                    )
                    true
                }
                else -> false
            }
        }
        configureSearchMenu(binding.toolbar.menu)

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        observe()
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(uid: String) {
        viewModel.enable(uid)
    }

    override fun disable(uid: String) {
        viewModel.disable(uid)
    }

    override fun edit(uid: String) {
        viewModel.onClickSettings(uid)
    }

    // CoinListBaseFragment

    override fun updateFilter(query: String) {
        viewModel.updateFilter(query)
    }

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner) { viewItems ->
            setViewItems(viewItems)
        }

        viewModel.disableCoinLiveData.observe(viewLifecycleOwner) {
            disableCoin(it)
        }

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

}
