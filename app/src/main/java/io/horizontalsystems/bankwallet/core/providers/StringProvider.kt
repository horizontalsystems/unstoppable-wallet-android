package io.horizontalsystems.bankwallet.core.providers

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.core.App

class StringProvider {

    private val context = App.instance.localizedContext()

    fun string(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun string(@StringRes id: Int, vararg params: Any): String {
        return context.getString(id, *params)
    }

}
