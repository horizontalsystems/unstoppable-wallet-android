package io.horizontalsystems.bankwallet.ui.extensions

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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

        (activity as? AppCompatActivity)?.let {
            it.setSupportActionBar(toolbar)
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        adapter = CoinListAdapter(this)
        recyclerView.adapter = adapter

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

    protected fun configureSearchMenu(menu: Menu, hint: Int) {
        val menuItem = menu.findItem(R.id.search) ?: return
        val searchView: SearchView = menuItem.actionView as SearchView
        searchView.maxWidth = Integer.MAX_VALUE
        searchView.queryHint = getString(hint)
        searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI

        searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)
        searchView.findViewById<EditText>(R.id.search_src_text)?.let { editText ->
            context?.getColor(R.color.grey_50)?.let { color -> editText.setHintTextColor(color) }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newQuery: String): Boolean {
                val trimmedQuery = newQuery.trim()
                updateFilter(trimmedQuery)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = false
        })
    }

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
