package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.ui.extensions.coinlist.CoinListBaseFragment
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_manage_wallets.*

class CreateWalletFragment : CoinListBaseFragment() {

    override val title
        get() = getString(R.string.ManageCoins_title)

    private lateinit var viewModel: CreateWalletViewModel
    private var doneMenuButton: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.inflateMenu(R.menu.create_wallet_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuDone -> {
                    hideKeyboard()
                    Handler(Looper.getMainLooper()).postDelayed({
                        viewModel.onCreate()
                    }, 100)
                    true
                }
                else -> false
            }
        }
        configureSearchMenu(toolbar.menu)
        doneMenuButton = toolbar.menu.findItem(R.id.menuDone)

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType")

        viewModel = ViewModelProvider(this, CreateWalletModule.Factory(predefinedAccountType))
                .get(CreateWalletViewModel::class.java)

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

    private fun observe() {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, Observer { state ->
            setViewState(state)
        })

        viewModel.canCreateLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            doneMenuButton?.let { menuItem ->
                setMenuItemEnabled(menuItem, enabled)
            }
        })

        viewModel.finishLiveEvent.observe(viewLifecycleOwner, Observer {
            findNavController().popBackStack()
        })

        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer {
            HudHelper.showErrorMessage(this.requireView(), getString(R.string.default_error_msg))
        })
    }

}
