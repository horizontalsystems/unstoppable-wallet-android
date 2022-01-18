package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.core.findNavController

class RestoreSelectCoinsFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.Restore_Title)

    private lateinit var viewModel: RestoreSelectCoinsViewModel
    private lateinit var coinSettingsViewModel: CoinSettingsViewModel
    private lateinit var restoreSettingsViewModel: RestoreSettingsViewModel
    private lateinit var coinPlatformsViewModel: CoinPlatformsViewModel

    private var doneMenuButton: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.inflateMenu(R.menu.restore_select_coin_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuRestore -> {
                    hideKeyboard()
                    viewModel.onRestore()
                    true
                }
                else -> false
            }
        }
        doneMenuButton = binding.toolbar.menu.findItem(R.id.menuRestore)

        val accountType = arguments?.getParcelable<AccountType>(ACCOUNT_TYPE_KEY)
            ?: throw Exception("Parameter missing")

        val vmFactory by lazy { RestoreSelectCoinsModule.Factory(accountType) }

        viewModel = ViewModelProvider(this, vmFactory).get(RestoreSelectCoinsViewModel::class.java)
        coinSettingsViewModel =
            ViewModelProvider(this, vmFactory).get(CoinSettingsViewModel::class.java)
        restoreSettingsViewModel =
            ViewModelProvider(this, vmFactory).get(RestoreSettingsViewModel::class.java)
        coinPlatformsViewModel =
            ViewModelProvider(this, vmFactory).get(CoinPlatformsViewModel::class.java)

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

    override fun updateFilter(query: String) {}

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner) { viewItems ->
            setViewItems(viewItems)
        }

        viewModel.disableBlockchainLiveData.observe(viewLifecycleOwner) {
            disableCoin(it)
        }

        viewModel.successLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack(R.id.restoreMnemonicFragment, true)
        }

        viewModel.restoreEnabledLiveData.observe(viewLifecycleOwner) { enabled ->
            doneMenuButton?.let { menuItem ->
                setMenuItemEnabled(menuItem, enabled)
            }
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
            val zcashBirthdayHeightDialog = ZcashBirthdayHeightDialog()
            zcashBirthdayHeightDialog.onEnter = {
                restoreSettingsViewModel.onEnter(it)
            }
            zcashBirthdayHeightDialog.onCancel = {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }

            zcashBirthdayHeightDialog.show(
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

    companion object {
        const val ACCOUNT_TYPE_KEY = "account_type_key"
    }
}
