package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

abstract class CoinListBaseFragment: BaseFragment(), CoinListAdapter.Listener {

    private lateinit var adapter: CoinListAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.manage_wallets_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CoinListAdapter(this)
        recyclerView.adapter = adapter

        searchView.bind(
                hint = getString(R.string.ManageCoins_Search),
                onTextChanged = { query ->
                    updateFilter(query)
                })
    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {}

    override fun disable(coin: Coin) {}

    override fun select(coin: Coin) {}

    // CoinListBaseFragment

    protected fun setItems(items: List<CoinViewItem>){
        adapter.viewItems = items
        adapter.notifyDataSetChanged()

        progressLoading.isVisible = false
    }

    abstract fun updateFilter(query: String)

    open fun onCancelAddressFormatSelection() {}

    open fun onSelectAddressFormat(coin: Coin, derivationSetting: DerivationSetting) {}

    protected fun showAddressFormatSelectionDialog(coin: Coin, currentDerivation: AccountType.Derivation) {
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
                    onSelectAddressFormat(coin, DerivationSetting(coin.type, items[position]))
                },
                onCancelled = {
                    onCancelAddressFormatSelection()
                }
        )
    }

}
