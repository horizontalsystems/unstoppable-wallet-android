package io.horizontalsystems.core.helpers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager


object KeyboardHelper {

    private fun showKeyboard(context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    fun showKeyboard(context: Context, view: View?) {
        view?.requestFocus()
        showKeyboard(context)
    }

    fun showKeyboardDelayed(context: Context, view: View?, delay: Long) {
        view?.postDelayed({ showKeyboard(context, view) }, delay)
    }

    fun hideKeyboard(context: Context, view: View?) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(view?.getWindowToken(), 0)
    }

}