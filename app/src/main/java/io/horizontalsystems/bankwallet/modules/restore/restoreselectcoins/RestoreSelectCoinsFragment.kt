package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms.CoinPlatformsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.coinsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsDialog
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class RestoreSelectCoinsFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.Select_Coins)

    private lateinit var viewModel: RestoreSelectCoinsViewModel
    private lateinit var coinSettingsViewModel: CoinSettingsViewModel
    private lateinit var restoreSettingsViewModel: RestoreSettingsViewModel
    private lateinit var coinPlatformsViewModel: CoinPlatformsViewModel

    private var doneMenuButton: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.restore_select_coin_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuDone -> {
                    hideKeyboard()
                    viewModel.onRestore()
                    true
                }
                else -> false
            }
        }
        configureSearchMenu(toolbar.menu)
        doneMenuButton = toolbar.menu.findItem(R.id.menuDone)

        val accountType = arguments?.getParcelable<AccountType>(ACCOUNT_TYPE_KEY)
            ?: throw Exception("Parameter missing")

        val vmFactory by lazy { RestoreSelectCoinsModule.Factory(accountType) }

        viewModel = ViewModelProvider(this, vmFactory).get(RestoreSelectCoinsViewModel::class.java)
        coinSettingsViewModel = ViewModelProvider(this, vmFactory).get(CoinSettingsViewModel::class.java)
        restoreSettingsViewModel = ViewModelProvider(this, vmFactory).get(RestoreSettingsViewModel::class.java)
        coinPlatformsViewModel = ViewModelProvider(this, vmFactory).get(CoinPlatformsViewModel::class.java)

        val enableCoinsViewModel by viewModels<EnableCoinsViewModel> { vmFactory }

        enableCoinsViewModel.confirmationLiveData.observe(viewLifecycleOwner, Observer { tokenType ->
            activity?.let {
                EnableCoinsDialog.show(it, tokenType, object : EnableCoinsDialog.Listener {
                    override fun onClickEnable() {
                        scrollToTopAfterUpdate = true
                        enableCoinsViewModel.onConfirmEnable()
                    }
                })
            }
        })

        enableCoinsViewModel.hudStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                EnableCoinsViewModel.HudState.Hidden -> {
                }
                EnableCoinsViewModel.HudState.Loading -> {
                    HudHelper.showInProcessMessage(
                        requireView(),
                        R.string.EnalbeToken_Enabling,
                        SnackbarDuration.INDEFINITE
                    )
                }
                EnableCoinsViewModel.HudState.Error -> {
                    HudHelper.showErrorMessage(requireView(), R.string.Error)
                }
                is EnableCoinsViewModel.HudState.Success -> {
                    if (state.count == 0) {
                        HudHelper.showSuccessMessage(requireView(), R.string.EnalbeToken_NoCoins)
                    } else {
                        HudHelper.showSuccessMessage(
                            requireView(),
                            getString(R.string.EnalbeToken_EnabledCoins, state.count)
                        )
                    }
                }
            }
        })

        observe()
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(fullCoin: FullCoin) {
        viewModel.enable(fullCoin)
    }

    override fun disable(fullCoin: FullCoin) {
        viewModel.disable(fullCoin)
    }

    override fun edit(fullCoin: FullCoin) {
        viewModel.onClickSettings(fullCoin)
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

            zcashBirthdayHeightDialog.show(requireActivity().supportFragmentManager, "ZcashBirthdayHeightDialog")
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
