package cash.p.terminal.strings.helpers

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class TranslatableString {
    class PlainString(val text: String) : TranslatableString()
    class ResString(@StringRes val id: Int, vararg val formatArgs: Any) : TranslatableString()

    @Composable
    fun getString(): String {
        return when (this) {
            is PlainString -> text
            is ResString -> stringResource(id, *formatArgs)
        }
    }

    override fun toString(): String {
        return when (this) {
            is PlainString -> text
            is ResString -> Translator.getString(id, formatArgs)
        }
    }
}

interface WithTranslatableTitle {
    val title: TranslatableString
}
