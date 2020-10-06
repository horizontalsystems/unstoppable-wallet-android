package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.addErc20token.AddErc20TokenFragment
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.noaccount.NoAccountDialog
import io.horizontalsystems.bankwallet.modules.restore.RestoreModule
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment

class ManageWalletsFragment : CoinListBaseFragment(), NoAccountDialog.Listener {

    companion object {
        fun start(activity: FragmentActivity) {
            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, ManageWalletsFragment())
                addToBackStack(null)
            }
        }
    }

    private lateinit var viewModel: ManageWalletsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.title  = getString(R.string.ManageCoins_title)
        }

        viewModel = ViewModelProvider(this, ManageWalletsModule.Factory())
                .get(ManageWalletsViewModel::class.java)

        observe()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is NoAccountDialog) {
            childFragment.setListener(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.manage_wallets_menu, menu)
        configureSearchMenu(menu, R.string.ManageCoins_Search)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuAddToken -> {
                hideKeyboard()
                showAddTokenDialog()
                return true
            }
            android.R.id.home -> {
                activity?.supportFragmentManager?.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun canHandleOnBackPress(): Boolean {
        activity?.supportFragmentManager?.popBackStack()
        return true
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

    //NoAccountDialog.Listener

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        activity?.let {
            RestoreModule.start(it, true, predefinedAccountType, false)
        }
    }

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            setViewState(state)
        })

        viewModel.openDerivationSettingsLiveEvent.observe(viewLifecycleOwner, Observer { (coin, currentDerivation) ->
            hideKeyboard()
            showAddressFormatSelectionDialog(coin, currentDerivation)
        })
    }

    private fun showAddTokenDialog() {
        hideKeyboard()
        activity?.let {
            AddTokenDialog.show(it, object : AddTokenDialog.Listener {
                override fun onClickAddErc20Token() {
                    activity?.let {
                        AddErc20TokenFragment.start(it)
                    }
                }
            })
        }
    }

}
