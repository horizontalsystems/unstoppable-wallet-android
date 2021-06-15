package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.blockchainsettings.CoinSettingsViewModel
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins.RestoreSettingsViewModel
import io.horizontalsystems.bankwallet.ui.extensions.ZcashBirthdayHeightDialog
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class ManageWalletsFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.ManageCoins_title)

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val coinSettingsViewModel by viewModels<CoinSettingsViewModel> { vmFactory }
    private val restoreSettingsViewModel by viewModels<RestoreSettingsViewModel> { vmFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.manage_wallets_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddToken -> {
                    hideKeyboard()
                    findNavController().navigate(R.id.manageWalletsFragment_to_addToken, null, navOptions())
                    true
                }
                else -> false
            }
        }
        configureSearchMenu(toolbar.menu)

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        observe()
    }

    override fun searchExpanded(menu: Menu) {
        menu.findItem(R.id.menuAddToken)?.isVisible = false
    }

    override fun searchCollapsed(menu: Menu) {
        menu.findItem(R.id.menuAddToken)?.isVisible = true
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {
        viewModel.enable(coin)
    }

    override fun disable(coin: Coin) {
        viewModel.disable(coin)
    }

    override fun edit(coin: Coin) {
        viewModel.onClickSettings(coin)
    }

    // CoinListBaseFragment

    override fun updateFilter(query: String) {
        viewModel.updateFilter(query)
    }

    override fun onCancelSelection() {
        coinSettingsViewModel.onCancelSelect()
    }

    override fun onSelect(indexes: List<Int>) {
        coinSettingsViewModel.onSelect(indexes)
    }

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            setViewState(state)
        })

        viewModel.disableCoinLiveData.observe(viewLifecycleOwner) {
            disableCoin(it)
        }

        coinSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner, Observer { config ->
            hideKeyboard()
            showBottomSelectorDialog(config)
        })

        restoreSettingsViewModel.openBirthdayAlertSignal.observe(viewLifecycleOwner) {
            val zcashBirhdayHeightDialog = ZcashBirthdayHeightDialog()
            zcashBirhdayHeightDialog.onEnter = {
                restoreSettingsViewModel.onEnter(it)
            }
            zcashBirhdayHeightDialog.onCancel = {
                restoreSettingsViewModel.onCancelEnterBirthdayHeight()
            }

            zcashBirhdayHeightDialog.show(requireActivity().supportFragmentManager, "ZcashBirhdayHeightDialog")
        }
    }

}
