package io.horizontalsystems.uikit

import android.view.View

//  View extensions

fun View.showIf(condition: Boolean, hideType: Int = View.GONE) {
    visibility = if (condition) View.VISIBLE else hideType
}
