package io.horizontalsystems.core

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Point
import android.os.Parcelable
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
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

//Animation

inline fun getValueAnimator(
        forward: Boolean = true,
        duration: Long,
        interpolator: TimeInterpolator,
        crossinline updateListener: (progress: Float) -> Unit
): ValueAnimator {
    val a =
            if (forward) ValueAnimator.ofFloat(0f, 1f)
            else ValueAnimator.ofFloat(1f, 0f)
    a.addUpdateListener { updateListener(it.animatedValue as Float) }
    a.duration = duration
    a.interpolator = interpolator
    return a
}

inline val Int.dp: Int
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics).toInt()

fun View.measureHeight(): Int {
    measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
    return measuredHeight
}