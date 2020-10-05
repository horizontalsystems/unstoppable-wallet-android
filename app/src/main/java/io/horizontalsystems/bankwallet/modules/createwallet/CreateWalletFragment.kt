package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.extensions.CoinListBaseFragment
import io.horizontalsystems.core.helpers.HudHelper

class CreateWalletFragment : CoinListBaseFragment() {

    companion object {
        fun instance(predefinedAccountType: PredefinedAccountType?, inApp: Boolean): CreateWalletFragment {
            return CreateWalletFragment().apply {
                arguments = Bundle(2).apply {
                    putParcelable("predefinedAccountType", predefinedAccountType)
                    putBoolean("inApp", inApp)
                }
            }
        }
    }

    private lateinit var viewModel: CreateWalletViewModel
    private var doneMenuButton: MenuItem? = null
    private var inApp = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.title  = getString(R.string.ManageCoins_title)
        }

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType")
        inApp = arguments?.getBoolean("inApp") ?: true

        viewModel = ViewModelProvider(this, CreateWalletModule.Factory(predefinedAccountType))
                .get(CreateWalletViewModel::class.java)

        observe()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.create_wallet_menu, menu)
        configureSearchMenu(menu, R.string.ManageCoins_Search)
        doneMenuButton = menu.findItem(R.id.menuDone)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuDone -> {
                hideKeyboard()
                Handler().postDelayed({
                    viewModel.onCreate()
                }, 100)
                return true
            }
            android.R.id.home -> {
                activity?.supportFragmentManager?.popBackStack()
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

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer { items ->
            setItems(items)
        })

        viewModel.canCreateLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            doneMenuButton?.let { menuItem ->
                setMenuItemEnabled(menuItem, enabled)
            }
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            closeWithSuccess()
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), getString(R.string.default_error_msg))
        })
    }

    private fun onBackPress() {
        hideKeyboard()
        parentFragmentManager.popBackStack()
    }

    private fun closeWithSuccess() {
        if (inApp) {
            onBackPress()
        } else {
            activity?.let {
                MainModule.start(it)
                it.finishAffinity()
            }
        }
    }

}
