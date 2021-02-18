package io.horizontalsystems.bankwallet.core

import android.view.Menu
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import io.horizontalsystems.bankwallet.R

abstract class BaseWithSearchDialogFragment : BaseDialogFragment(), SearchViewHelper.Listener {

    abstract override fun updateFilter(query: String)
    override fun searchExpanded(menu: Menu) {}
    override fun searchCollapsed(menu: Menu) {}

    private var searchView: SearchView? = null

    protected fun configureSearchMenu(menu: Menu, hint: Int = R.string.ManageCoins_Search) {
        context?.let { context ->
            SearchViewHelper.configureSearchMenu(context, menu, context.getString(hint), this)
        }
    }

    override fun onDetach() {
        super.onDetach()
        hideKeyboard()
        searchView?.setOnQueryTextListener(null)
    }

    private fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }
}
