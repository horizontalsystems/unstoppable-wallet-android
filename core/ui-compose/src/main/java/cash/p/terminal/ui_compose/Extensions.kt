package cash.p.terminal.ui_compose

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

//  Fragment

fun Fragment.findNavController(): NavController {
    return NavHostFragment.findNavController(this)
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.getInputX(): T? {
    return parcelable("input")
}

inline fun <reified T : Parcelable> NavController.getInput(): T? {
    return currentBackStackEntry?.arguments?.getInputX()
}

inline fun <reified T: Parcelable> NavController.requireInput() : T {
    return getInput()!!
}

