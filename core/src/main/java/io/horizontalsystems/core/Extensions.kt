package io.horizontalsystems.core

import android.content.Intent
import android.os.Parcelable
import android.view.View
import io.horizontalsystems.core.helpers.SingleClickListener

//  View

fun View.setOnSingleClickListener(l: ((v: View) -> Unit)) {
    this.setOnClickListener(object : SingleClickListener() {
        override fun onSingleClick(v: View) {
            l.invoke(v)
        }
    })
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
