package io.horizontalsystems.bankwallet.viewHelpers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

//  LayoutInflater

fun inflate(parent: ViewGroup, layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
}
