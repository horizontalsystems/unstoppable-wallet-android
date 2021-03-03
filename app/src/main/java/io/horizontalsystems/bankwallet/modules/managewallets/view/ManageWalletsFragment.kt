package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenFragment
import io.horizontalsystems.bankwallet.modules.addtoken.TokenType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsDialog
import io.horizontalsystems.bankwallet.modules.enablecoins.EnableCoinsViewModel
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsViewModel
import io.horizontalsystems.bankwallet.modules.noaccount.NoAccountDialog
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class ManageWalletsFragment : CoinListBaseFragment(), NoAccountDialog.Listener {

    override val title
        get() = getString(R.string.ManageCoins_title)

    private val vmFactory by lazy { ManageWalletsModule.Factory() }
    private val viewModel by viewModels<ManageWalletsViewModel> { vmFactory }
    private val blockchainSettingsViewModel by viewModels<BlockchainSettingsViewModel> { vmFactory }
    private val enableCoinsViewModel by viewModels<EnableCoinsViewModel> { vmFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.manage_wallets_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuAddToken -> {
                    hideKeyboard()
                    showAddTokenDialog()
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

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is NoAccountDialog) {
            childFragment.setListener(this)
        }
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

    override fun select(coin: Coin) {
        NoAccountDialog.show(childFragmentManager, coin)
        viewModel.onAddAccount(coin)
    }

    // CoinListBaseFragment

    override fun updateFilter(query: String) {
        viewModel.updateFilter(query)
    }

    override fun onCancelSelection() {
        blockchainSettingsViewModel.onCancelSelect()
    }

    override fun onSelect(index: Int) {
        blockchainSettingsViewModel.onSelect(index)
    }

    // NoAccountDialog.Listener

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType, coin: Coin) {
        val arguments = Bundle(3).apply {
            putParcelable(RestoreFragment.PREDEFINED_ACCOUNT_TYPE_KEY, predefinedAccountType)
            putParcelable(RestoreFragment.COIN_TO_ENABLE, coin)
            putBoolean(RestoreFragment.SELECT_COINS_KEY, false)
        }

        findNavController().navigate(R.id.manageWalletsFragment_to_restoreFragment, arguments, navOptions())
    }

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            setViewState(state)
        })

        blockchainSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner, Observer { config ->
            hideKeyboard()
            showBottomSelectorDialog(config)
        })

        enableCoinsViewModel.confirmationLiveData.observe(viewLifecycleOwner, Observer { tokenType ->
            activity?.let {
                EnableCoinsDialog.show(it, tokenType, object: EnableCoinsDialog.Listener {
                    override fun onClickEnable() {
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
                    HudHelper.showInProcessMessage(requireView(), R.string.EnalbeToken_Enabling)
                }
                EnableCoinsViewModel.HudState.Error -> {
                    HudHelper.showErrorMessage(requireView(), R.string.Error)
                }
                is EnableCoinsViewModel.HudState.Success -> {
                    if (state.count == 0) {
                        HudHelper.showSuccessMessage(requireView(), R.string.EnalbeToken_NoCoins)
                    } else {
                        HudHelper.showSuccessMessage(requireView(), getString(R.string.EnalbeToken_EnabledCoins, state.count))
                    }
                }
            }
        })
    }

    private fun showAddTokenDialog() {
        hideKeyboard()
        activity?.let {
            AddTokenDialog.show(it, object : AddTokenDialog.Listener {
                override fun onClickAddErc20Token() {
                    openAddToken(TokenType.Erc20)
                }

                override fun onClickAddBep20Token() {
                    openAddToken(TokenType.Bep20)
                }

                override fun onClickAddBep2Token() {
                    openAddToken(TokenType.Bep2)
                }
            })
        }
    }

    private fun openAddToken(tokenType: TokenType) {
        val arguments = Bundle(1).apply {
            putParcelable(AddTokenFragment.TOKEN_TYPE_KEY, tokenType)
        }
        findNavController().navigate(R.id.manageWalletsFragment_to_addToken, arguments, navOptions())
    }
}
