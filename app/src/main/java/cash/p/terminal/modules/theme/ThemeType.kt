package cash.p.terminal.modules.theme

import cash.p.terminal.R
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.WithTranslatableTitle

enum class ThemeType(
    val value: String,
    override val title: TranslatableString,
    val iconRes: Int
) : WithTranslatableTitle {
    Dark(
        "Dark",
        TranslatableString.ResString(R.string.SettingsTheme_Dark),
        R.drawable.ic_theme_dark
    ),
    Light(
        "Light",
        TranslatableString.ResString(R.string.SettingsTheme_Light),
        R.drawable.ic_theme_light
    ),
    System(
        "System",
        TranslatableString.ResString(R.string.SettingsTheme_System),
        R.drawable.ic_theme_system
    );
}
