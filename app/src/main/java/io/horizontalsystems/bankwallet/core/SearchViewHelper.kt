package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.graphics.Color
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import io.horizontalsystems.bankwallet.R

object SearchViewHelper {

    interface Listener {
        fun updateFilter(query: String)
        fun searchExpanded(menu: Menu)
        fun searchCollapsed(menu: Menu)
    }

    fun configureSearchMenu(context: Context, menu: Menu, hint: String, listener: Listener): SearchView? {
        val menuItem = menu.findItem(R.id.search) ?: return null
        val searchView = menuItem.actionView as? SearchView

        searchView?.let {
            searchView.maxWidth = Integer.MAX_VALUE
            searchView.queryHint = hint
            searchView.imeOptions = searchView.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI

            searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)
            searchView.findViewById<EditText>(R.id.search_src_text)?.let { editText ->
                context.getColor(R.color.grey_50).let { color -> editText.setHintTextColor(color) }
            }

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newQuery: String): Boolean {
                    val trimmedQuery = newQuery.trim()
                    listener.updateFilter(trimmedQuery)
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean = false
            })
        }

        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                listener.searchExpanded(menu)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                listener.searchCollapsed(menu)
                return true
            }
        })

        return searchView
    }

}
