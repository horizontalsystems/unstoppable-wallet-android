package io.horizontalsystems.walletkit.modules.theme

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle

enum class ThemeType(
    val value: String,
    override val title: TranslatableString,
    val iconRes: Int
) : WithTranslatableTitle {
    @SerializedName("dark")
    Dark(
        "Dark",
        TranslatableString.ResString(R.string.SettingsTheme_Dark),
        R.drawable.ic_theme_dark
    ),
    @SerializedName("light")
    Light(
        "Light",
        TranslatableString.ResString(R.string.SettingsTheme_Light),
        R.drawable.ic_theme_light
    ),
    @SerializedName("system")
    System(
        "System",
        TranslatableString.ResString(R.string.SettingsTheme_System),
        R.drawable.ic_theme_system
    );
}
