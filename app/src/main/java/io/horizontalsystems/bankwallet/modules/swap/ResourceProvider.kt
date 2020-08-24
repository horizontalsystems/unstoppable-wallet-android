package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import android.content.res.Resources
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import io.horizontalsystems.views.R
import io.horizontalsystems.views.helpers.LayoutHelper

class ResourceProvider(
        private val context: Context
) {

    private val theme: Resources.Theme
        get() = context.theme

    fun string(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun color(@ColorRes id: Int): Int {
        return context.getColor(id)
    }

    fun colorLucian(): Int {
        return LayoutHelper.getAttr(R.attr.ColorLucian, theme)
                ?: color(R.color.red_d)
    }

    fun colorRemus(): Int {
        return LayoutHelper.getAttr(R.attr.ColorRemus, theme)
                ?: color(R.color.green_d)
    }

    fun colorJacob(): Int {
        return LayoutHelper.getAttr(R.attr.ColorJacob, theme)
                ?: color(R.color.yellow_d)
    }

}
