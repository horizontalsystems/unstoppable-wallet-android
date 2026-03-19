package io.horizontalsystems.bankwallet.modules.nav3

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavOptions

class NavController {

    fun removeLastOrNull() {
        TODO()
    }

    fun removeLastUntil(i: Int, b: Boolean) {
        TODO()
    }

    fun navigate(resId: Int, args: Bundle?, navOptions: NavOptions) {
        TODO()
    }

    val previousBackStackEntry: NavBackStackEntry?
        get() = TODO()
    val currentBackStackEntry: NavBackStackEntry?
        get() = TODO()

    fun getBackStackEntry(@IdRes destinationId: Int): NavBackStackEntry {
        TODO()
    }
}