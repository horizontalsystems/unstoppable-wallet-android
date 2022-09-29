package io.horizontalsystems.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment


fun View.hideKeyboard(context: Context) {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun NavController.setNavigationResult(key: String, bundle: Bundle, destinationId: Int? = null) {
    val backStackEntry = when (destinationId) {
        null -> previousBackStackEntry
        else -> backStack.findLast { it.destination.id == destinationId }
    }

    backStackEntry?.savedStateHandle?.set(key, bundle)
}

fun NavController.getNavigationResult(keyResult: String, onResult: (Bundle) -> Unit) {
    currentBackStackEntry?.let { backStackEntry ->
        backStackEntry.savedStateHandle.getLiveData<Bundle>(keyResult).observe(backStackEntry) {
            onResult.invoke(it)

            backStackEntry.savedStateHandle.remove<Bundle>(keyResult)
        }
    }
}

//  Fragment

fun Fragment.findNavController(): NavController {
    return NavHostFragment.findNavController(this)
}

fun Fragment.getNavigationResult(key: String = "result", result: (Bundle) -> (Unit)) {
    findNavController().getNavigationResult(key, result)
}

fun Fragment.setNavigationResult(key: String = "result", bundle: Bundle) {
    findNavController().setNavigationResult(key, bundle)
}

//  String

fun String.hexToByteArray(): ByteArray {
    return ByteArray(this.length / 2) {
        this.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

//  ByteArray

fun ByteArray.toHexString(): String {
    return this.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    }
}

//  Intent & Parcelable Enum

fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}

inline val Int.dp: Int
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()
