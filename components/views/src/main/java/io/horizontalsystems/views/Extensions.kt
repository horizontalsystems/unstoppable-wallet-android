package io.horizontalsystems.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

//  View extensions

fun inflate(parent: ViewGroup, layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
}
