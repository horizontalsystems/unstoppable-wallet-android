package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.CoinListBaseFragment

class RestoreSelectCoinsFragment : CoinListBaseFragment() {

    companion object {
        fun instance(predefinedAccountType: PredefinedAccountType): RestoreSelectCoinsFragment {
            return RestoreSelectCoinsFragment().apply {
                arguments = Bundle(1).apply {
                    putParcelable("predefinedAccountType", predefinedAccountType)
                }
            }
        }
    }

    private lateinit var viewModel: RestoreSelectCoinsViewModel
    private var doneMenuButton: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.title  = getString(R.string.Select_Coins)
        }

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType") ?: throw Exception("Parameter missing")

        viewModel = ViewModelProvider(this, RestoreSelectCoinsModule.Factory(predefinedAccountType))
                .get(RestoreSelectCoinsViewModel::class.java)

        observe()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.restore_select_coin_menu, menu)
        configureSearchMenu(menu, R.string.ManageCoins_Search)
        doneMenuButton = menu.findItem(R.id.menuDone)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDone -> {
                hideKeyboard()
                viewModel.onRestore()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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

    override fun onCancelAddressFormatSelection() {
       viewModel.onCancelDerivationSelection()
    }

    override fun onSelectAddressFormat(coin: Coin, derivationSetting: DerivationSetting) {
        viewModel.onSelect(coin, derivationSetting)
    }

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer { items ->
            setItems(items)
        })

        viewModel.openDerivationSettingsLiveEvent.observe(viewLifecycleOwner, Observer { (coin, currentDerivation) ->
            hideKeyboard()
            showAddressFormatSelectionDialog(coin, currentDerivation)
        })

        viewModel.canRestoreLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            doneMenuButton?.let { menuItem ->
                setMenuItemEnabled(menuItem, enabled)
            }
        })

        viewModel.enabledCoinsLiveData.observe(viewLifecycleOwner, Observer { enabledCoins ->
            setFragmentResult(RestoreFragment.selectCoinsRequestKey, bundleOf(RestoreFragment.selectCoinsBundleKey to enabledCoins))
        })
    }

}
