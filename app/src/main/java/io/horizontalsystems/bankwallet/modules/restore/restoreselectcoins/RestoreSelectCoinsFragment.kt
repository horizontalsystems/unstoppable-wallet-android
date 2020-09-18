package io.horizontalsystems.bankwallet.modules.restore.restoreselectcoins

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.managewallets.view.ManageWalletItemsAdapter
import io.horizontalsystems.bankwallet.modules.restore.RestoreFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

class RestoreSelectCoinsFragment : BaseFragment(), ManageWalletItemsAdapter.Listener {

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
    private lateinit var adapter: ManageWalletItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.manage_wallets_fragment, container, false)
    }

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

        adapter = ManageWalletItemsAdapter(this)
        recyclerView.adapter = adapter

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    viewModel.updateFilter(query)
                })

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

    private fun observe() {
        viewModel.viewItemsLiveData.observe(viewLifecycleOwner, Observer { items ->
            adapter.viewItems = items
            adapter.notifyDataSetChanged()

            progressLoading.isVisible = false
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

    private fun showAddressFormatSelectionDialog(coin: Coin, currentDerivation: AccountType.Derivation) {
        val items = AccountType.Derivation.values().toList()
        val coinDrawable = context?.let { AppLayoutHelper.getCoinDrawable(it, coin.code, coin.type) }
                ?: return

        BottomSheetSelectorDialog.show(
                childFragmentManager,
                getString(R.string.AddressFormatSettings_Title),
                coin.title,
                coinDrawable,
                items.map { derivation -> Pair(derivation.longTitle(), getString(derivation.description(), derivation.addressPrefix(coin.type))) },
                items.indexOf(currentDerivation),
                notifyUnchanged = true,
                onItemSelected = { position ->
                    viewModel.onSelect(coin, DerivationSetting(coin.type, items[position]))
                },
                onCancelled = {
                    viewModel.onCancelDerivationSelection()
                }
        )
    }

}
