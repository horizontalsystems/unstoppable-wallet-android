package io.horizontalsystems.views

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import io.horizontalsystems.views.helpers.LayoutHelper

//  View extensions

fun View.showIf(condition: Boolean, hideType: Int = View.GONE) {
    visibility = if (condition) View.VISIBLE else hideType
}

fun ImageView.setCoinImage(coinCode: String) {
    setImageResource(LayoutHelper.getCoinDrawableResource(context, coinCode))

    val greyColor = ContextCompat.getColor(context, R.color.grey)
    val tintColorStateList = ColorStateList.valueOf(greyColor)
    imageTintList = tintColorStateList
}

fun inflate(parent: ViewGroup, layout: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(parent.context).inflate(layout, parent, attachToRoot)
}
