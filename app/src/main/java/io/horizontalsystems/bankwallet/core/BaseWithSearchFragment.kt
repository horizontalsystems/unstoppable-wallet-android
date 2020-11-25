package io.horizontalsystems.bankwallet.core

import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import io.horizontalsystems.bankwallet.R


abstract class BaseWithSearchFragment : BaseFragment() {

    abstract fun updateFilter(query: String)
    open fun searchExpanded(menu: Menu) {}
    open fun searchCollapsed(menu: Menu){}

    private var searchView: SearchView? = null

    protected fun configureSearchMenu(menu: Menu, hint: Int) {
        val menuItem = menu.findItem(R.id.search) ?: return
        searchView = menuItem.actionView as? SearchView
        searchView?.let { searchView ->
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

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                searchExpanded(menu)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                searchCollapsed(menu)
                return true
            }
        })
    }

    override fun onDetach() {
        super.onDetach()

        searchView?.setOnQueryTextListener(null)
    }
}
