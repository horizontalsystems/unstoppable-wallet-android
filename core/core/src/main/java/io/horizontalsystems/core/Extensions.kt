package io.horizontalsystems.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.NavController
import io.horizontalsystems.core.entities.BlockchainType
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue

val BlockchainType.imageUrl: String
    get() = "https://cdn.blocksdecoded.com/blockchain-icons/32px/$uid@3x.png"

fun View.hideKeyboard(context: Context) {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun NavController.setNavigationResult(key: String, bundle: Bundle, destinationId: Int? = null) {
    val backStackEntry = when (destinationId) {
        null -> previousBackStackEntry
        else -> currentBackStack.value.findLast { it.destination.id == destinationId }
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

inline fun <reified T: Parcelable> NavController.getInput() : T? {
    return currentBackStackEntry?.arguments?.getInputX()
}

inline fun <reified T: Parcelable> Bundle.getInputX() : T? {
    return parcelable("input")
}


inline fun <reified T: Parcelable> NavController.requireInput() : T {
    return getInput()!!
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

fun BigDecimal.scaleUp(scale: Int): BigInteger {
    val exponent = scale - scale()

    return if (exponent >= 0) {
        unscaledValue() * BigInteger.TEN.pow(exponent)
    } else {
        unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
    }
}

fun Double.smartFormat(): String {
    val hasDecimalPart = this % 1 != 0.0
    return if (hasDecimalPart) {
        String.format("%.1f", this)
    } else {
        String.format("%.0f", this)
    }
}