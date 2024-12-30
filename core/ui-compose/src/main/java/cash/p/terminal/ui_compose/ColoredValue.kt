package cash.p.terminal.ui_compose

import androidx.compose.runtime.Composable
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

data class ColoredValue(val value: String, val color: ColorName)

enum class ColorName {
    Remus, Lucian, Grey, Leah;

    @Composable
    fun compose() = when (this) {
        Remus -> ComposeAppTheme.colors.remus
        Lucian -> ComposeAppTheme.colors.lucian
        Leah -> ComposeAppTheme.colors.leah
        Grey -> ComposeAppTheme.colors.grey
    }
}