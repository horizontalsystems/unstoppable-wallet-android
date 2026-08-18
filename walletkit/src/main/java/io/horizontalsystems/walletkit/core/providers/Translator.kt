package io.horizontalsystems.walletkit.core.providers

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import io.horizontalsystems.walletkit.core.App

object Translator {

    fun getString(@StringRes id: Int): String {
        return App.instance.localizedContext().getString(id)
    }

    fun getString(@StringRes id: Int, vararg params: Any): String {
        return App.instance.localizedContext().getString(id, *params)
    }

    fun getQuantityString(@PluralsRes id: Int, quantity: Int, vararg params: Any): String {
        return App.instance.localizedContext().resources.getQuantityString(id, quantity, *params)
    }
}
