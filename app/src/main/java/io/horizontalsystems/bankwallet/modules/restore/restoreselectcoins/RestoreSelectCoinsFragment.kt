package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsViewModel
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class RestoreSelectCoinsFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.Select_Coins)

    private lateinit var viewModel: RestoreSelectCoinsViewModel
    private lateinit var blockchainSettingsViewModel: BlockchainSettingsViewModel

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
        configureSearchMenu(toolbar.menu, R.string.ManageCoins_Search)
        doneMenuButton = toolbar.menu.findItem(R.id.menuDone)

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>(PREDEFINED_ACCOUNT_TYPE_KEY) ?: throw Exception("Parameter missing")

        val vmFactory by lazy { RestoreSelectCoinsModule.Factory(predefinedAccountType) }

        viewModel = ViewModelProvider(this, vmFactory).get(RestoreSelectCoinsViewModel::class.java)
        blockchainSettingsViewModel = ViewModelProvider(this, vmFactory).get(BlockchainSettingsViewModel::class.java)

        observe()
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {
        viewModel.enable(coin)
    }

    override fun disable(coin: Coin) {
        viewModel.disable(coin)
    }

    override fun select(coin: Coin) {
        //not used here
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

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { viewState ->
            setViewState(viewState)
        })

        viewModel.canRestoreLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            doneMenuButton?.let { menuItem ->
                setMenuItemEnabled(menuItem, enabled)
            }
        })

        viewModel.enabledCoinsLiveData.observe(viewLifecycleOwner, Observer { enabledCoins ->
            setFragmentResult(RestoreFragment.selectCoinsRequestKey, bundleOf(RestoreFragment.selectCoinsBundleKey to enabledCoins))
        })

        blockchainSettingsViewModel.openBottomSelectorLiveEvent.observe(viewLifecycleOwner, Observer { config ->
            hideKeyboard()
            showBottomSelectorDialog(config)
        })
    }


    companion object {
        const val PREDEFINED_ACCOUNT_TYPE_KEY = "predefined_account_type_key"

        fun instance(predefinedAccountType: PredefinedAccountType): RestoreSelectCoinsFragment {
            return RestoreSelectCoinsFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable(PREDEFINED_ACCOUNT_TYPE_KEY, predefinedAccountType)
                }
            }
        }
    }

}
