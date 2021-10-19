package io.horizontalsystems.bankwallet.ui.compose

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed class TranslatableString {
    class PlainString(val text: String) : TranslatableString()
    class ResString(@StringRes val id: Int, val formatArgs: List<Any> = listOf()) : TranslatableString()

    @Composable
    fun getString(): String {
        return when (this) {
            is PlainString -> text
            is ResString -> stringResource(id, formatArgs)
        }
    }
}

interface WithTranslatableTitle {
    val title: TranslatableString
}
