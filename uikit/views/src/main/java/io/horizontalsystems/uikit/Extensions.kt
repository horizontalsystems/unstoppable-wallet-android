package io.horizontalsystems.uikit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

//  View extensions

fun View.showIf(condition: Boolean, hideType: Int = View.GONE) {
    visibility = if (condition) View.VISIBLE else hideType
}

fun inflate(parent: ViewGroup, layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
}
