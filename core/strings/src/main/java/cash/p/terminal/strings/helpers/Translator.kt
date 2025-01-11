package cash.p.terminal.strings.helpers

import androidx.annotation.StringRes

object Translator {

    fun getString(@StringRes id: Int): String {
        return getLocalAwareContext().getString(id)
    }

    fun getString(@StringRes id: Int, vararg params: Any): String {
        return getLocalAwareContext().getString(id, *params)
    }

    private fun getLocalAwareContext() =
        LocaleHelper.onAttach(LibraryInitializer.getApplicationContext())
}
