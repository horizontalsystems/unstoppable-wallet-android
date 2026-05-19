package cash.p.terminal.strings.helpers

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes

object Translator {
    @Volatile
    private var cachedContext: ContextWithLocale? = null

    fun getString(@StringRes id: Int): String {
        return runCatching {
            getLocalAwareContext().getString(id)
        }.getOrElse { "Preview mode" }
    }

    fun getString(@StringRes id: Int, vararg params: Any): String {
        return runCatching {
            getLocalAwareContext().getString(id, *params)
        }.getOrElse { "Preview mode" }
    }

    fun getStringArrayItem(@ArrayRes arrayId: Int, index: Int): String {
        return runCatching {
            getLocalAwareContext().resources.getStringArray(arrayId)[index]
        }.getOrElse { "Preview mode" }
    }

    private fun getLocalAwareContext(): Context {
        val applicationContext = LibraryInitializer.getApplicationContext()
        val localeTag = LocaleHelper.getLocale(applicationContext).toLanguageTag()
        cachedContext?.takeIf { it.localeTag == localeTag }?.let {
            return it.context
        }

        return synchronized(this) {
            cachedContext?.takeIf { it.localeTag == localeTag }?.context
                ?: LocaleHelper.onAttach(applicationContext).also {
                    cachedContext = ContextWithLocale(it, localeTag)
                }
        }
    }

    private data class ContextWithLocale(
        val context: Context,
        val localeTag: String,
    )
}
