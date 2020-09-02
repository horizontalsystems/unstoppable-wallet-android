package io.horizontalsystems.bankwallet.modules.swap

import android.content.Context
import androidx.annotation.StringRes

class StringProvider(
        private val context: Context
) {

    fun string(@StringRes id: Int): String {
        return context.getString(id)
    }

    fun string(@StringRes id: Int, vararg params: Any): String {
        return context.getString(id, *params)
    }

}
