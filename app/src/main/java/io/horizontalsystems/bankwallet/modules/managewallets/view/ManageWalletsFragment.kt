package io.horizontalsystems.bankwallet.modules.managewallets.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.modules.addErc20token.AddErc20TokenActivity
import io.horizontalsystems.bankwallet.modules.managewallets.ManageWalletsModule
import io.horizontalsystems.bankwallet.modules.noaccount.NoAccountDialog
import io.horizontalsystems.bankwallet.modules.restore.RestoreActivity
import io.horizontalsystems.bankwallet.modules.restore.RestoreMode
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

class ManageWalletsFragment : BaseFragment(), ManageWalletItemsAdapter.Listener, NoAccountDialog.Listener {

    companion object {
        fun start(activity: FragmentActivity) {
            activity.supportFragmentManager.commit {
                add(R.id.fragmentContainerView, ManageWalletsFragment())
                addToBackStack(null)
            }
        }
    }

    private lateinit var viewModel: ManageWalletsViewModel
    private lateinit var adapter: ManageWalletItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.manage_wallets_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        shadowlessToolbar.bind(
                getString(R.string.ManageCoins_title),
                leftBtnItem = TopMenuItem(text = R.string.ManageCoins_AddToken, onClick = { showAddTokenDialog() }),
                rightBtnItem = TopMenuItem(text = R.string.Button_Done, onClick = {
                    hideKeyboard()
                    Handler().postDelayed({
                        activity?.onBackPressed()
                    }, 100)
                })
        )

        viewModel = ViewModelProvider(this, ManageWalletsModule.Factory())
                .get(ManageWalletsViewModel::class.java)

        adapter = ManageWalletItemsAdapter(this)
        recyclerView.adapter = adapter

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    viewModel.updateFilter(query)
                })

        observe()
    }

    override fun onAttachFragment(childFragment: Fragment) {
        if (childFragment is NoAccountDialog) {
            childFragment.setListener(this)
        }
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

    //NoAccountDialog.Listener

    override fun onClickRestoreKey(predefinedAccountType: PredefinedAccountType) {
        startActivity(Intent(context, RestoreActivity::class.java).apply {
            putParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType)
            putParcelableExtra(ModuleField.RESTORE_MODE, RestoreMode.InApp)
        })
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

    private fun showAddTokenDialog() {
        hideKeyboard()
        activity?.let {
            AddTokenDialog.show(it, object : AddTokenDialog.Listener {
                override fun onClickAddErc20Token() {
                    startActivity(Intent(activity, AddErc20TokenActivity::class.java))
                }
            })
        }
    }

    private fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }
}
