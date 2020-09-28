package io.horizontalsystems.bankwallet.modules.createwallet

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.main.MainModule
import io.horizontalsystems.bankwallet.ui.extensions.CoinListBaseFragment
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

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
    private var inApp = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPress() },
                rightBtnItem = TopMenuItem(text = R.string.Button_Create, onClick = {
                    hideKeyboard()
                    Handler().postDelayed({
                        viewModel.onCreate()
                    }, 100)
                })
        )
        //disable create button
        shadowlessToolbar.setRightButtonEnabled(false)

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType")
        inApp = arguments?.getBoolean("inApp") ?: true

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
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer { items ->
            setItems(items)
        })

        viewModel.canCreateLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            shadowlessToolbar.setRightButtonEnabled(enabled)
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
