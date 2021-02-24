package io.horizontalsystems.bankwallet.core

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.core.hideKeyboard

abstract class BaseActivity : CoreActivity(), NavController.OnDestinationChangedListener {

    protected fun hideSoftKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    // NavController Listener

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        currentFocus?.hideKeyboard(this)
    }
}
