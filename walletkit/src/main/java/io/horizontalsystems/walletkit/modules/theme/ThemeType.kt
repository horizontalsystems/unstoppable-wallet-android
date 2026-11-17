package io.horizontalsystems.walletkit.modules.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
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

    /**
     * Makes the app light, dark or follow the phone.
     *
     * From Android 12 the choice is stored with the system, because the
     * system draws the splash screen before the app runs and would otherwise
     * draw it in the phone's mode. Older versions have no such store: the
     * choice is applied inside the app only, and the splash screen follows
     * the phone.
     */
    fun applyNightMode(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val nightMode = when (this) {
                Light -> UiModeManager.MODE_NIGHT_NO
                Dark -> UiModeManager.MODE_NIGHT_YES
                // Clears the app's own mode, so the phone's applies
                System -> UiModeManager.MODE_NIGHT_AUTO
            }
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(nightMode)
        } else {
            val nightMode = when (this) {
                Light -> AppCompatDelegate.MODE_NIGHT_NO
                Dark -> AppCompatDelegate.MODE_NIGHT_YES
                System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (AppCompatDelegate.getDefaultNightMode() != nightMode) {
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }
    }
}
