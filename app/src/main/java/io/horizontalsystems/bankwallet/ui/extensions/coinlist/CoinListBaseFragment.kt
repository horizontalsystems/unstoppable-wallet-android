package io.horizontalsystems.bankwallet.ui.extensions.coinlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetSelectorDialog
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import kotlinx.android.synthetic.main.manage_wallets_fragment.*

abstract class CoinListBaseFragment: BaseWithSearchFragment(), CoinListAdapter.Listener {

    private lateinit var featuredItemsAdapter: CoinListAdapter
    private lateinit var itemsAdapter: CoinListAdapter

    abstract val title: CharSequence

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.manage_wallets_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.title = title
        }

        featuredItemsAdapter = CoinListAdapter(this)
        itemsAdapter = CoinListAdapter(this)
        recyclerView.itemAnimator = null
        recyclerView.adapter = ConcatAdapter(featuredItemsAdapter, itemsAdapter)

    }

    // ManageWalletItemsAdapter.Listener

    override fun enable(coin: Coin) {}

    override fun disable(coin: Coin) {}

    override fun select(coin: Coin) {}

    // CoinListBaseFragment

    protected fun setViewState(viewState: CoinViewState){
        featuredItemsAdapter.submitList(viewState.featuredViewItems)
        itemsAdapter.submitList(viewState.viewItems)

        progressLoading.isVisible = false
    }

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
