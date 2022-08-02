package io.horizontalsystems.bankwallet.core

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import io.horizontalsystems.bankwallet.R

abstract class BaseFragment(@LayoutRes layoutResId: Int = 0) : Fragment(layoutResId) {

    protected fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    protected fun setMenuItemEnabled(menuItem: MenuItem, enabled: Boolean) {
        menuItem.isEnabled = enabled
        context?.let { ctx ->
            val color = ctx.getColor(if (enabled) R.color.jacob else R.color.grey)
            val spannable = SpannableString(menuItem.title)
            spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
            menuItem.title = spannable
        }
    }

    protected fun allowScreenshot() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    protected fun disallowScreenshot() {
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

}
