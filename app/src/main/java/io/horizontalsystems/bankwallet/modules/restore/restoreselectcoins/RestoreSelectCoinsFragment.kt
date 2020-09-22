package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.CoinListBaseFragment
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { parentFragmentManager.popBackStack() },
                rightBtnItem = TopMenuItem(text = R.string.Button_Restore, onClick = {
                    hideKeyboard()
                    Handler().postDelayed({
                        viewModel.onRestore()
                    }, 100)
                })
        )
        //disable restore button
        shadowlessToolbar.setRightButtonEnabled(false)

        val predefinedAccountType = arguments?.getParcelable<PredefinedAccountType>("predefinedAccountType") ?: throw Exception("Parameter missing")

        viewModel = ViewModelProvider(this, RestoreSelectCoinsModule.Factory(predefinedAccountType))
                .get(RestoreSelectCoinsViewModel::class.java)

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
            shadowlessToolbar.setRightButtonEnabled(enabled)
        })

        viewModel.enabledCoinsLiveData.observe(viewLifecycleOwner, Observer { enabledCoins ->
            setFragmentResult(RestoreFragment.selectCoinsRequestKey, bundleOf(RestoreFragment.selectCoinsBundleKey to enabledCoins))
        })
    }

}
