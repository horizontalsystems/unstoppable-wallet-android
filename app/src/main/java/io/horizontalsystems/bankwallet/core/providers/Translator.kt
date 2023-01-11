package io.horizontalsystems.bankwallet.core.providers

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.core.App

object Translator {

    fun getString(@StringRes id: Int): String {
        return App.instance.localizedContext().getString(id)
    }

    fun getString(@StringRes id: Int, vararg params: Any): String {
        return App.instance.localizedContext().getString(id, *params)
    }
}
