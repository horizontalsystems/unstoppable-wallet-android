package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object AppearanceModule {

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val launchScreenService = LaunchScreenService(App.localStorage)
            val appIconService = AppIconService(App.localStorage)
            val themeService = ThemeService(App.localStorage)
            return AppearanceViewModel(
                launchScreenService,
                appIconService,
                themeService,
                App.baseTokenManager,
                App.balanceViewTypeManager,
                App.localStorage,
                App.balanceHiddenManager
            ) as T
        }
    }

}

enum class AppIcon(val icon: Int, val titleText: String) : WithTranslatableTitle {
    Main(R.drawable.launcher_main_preview, "Main"),
    Dark(R.drawable.launcher_dark_preview, "Dark"),
    Mono(R.drawable.launcher_mono_preview, "Mono"),
    Leo(R.drawable.launcher_leo_preview, "Leo"),
    Mustang(R.drawable.launcher_mustang_preview, "Mustang"),
    Yak(R.drawable.launcher_yak_preview, "Yak"),
    Punk(R.drawable.launcher_punk_preview, "Punk"),
    Ape(R.drawable.launcher_ape_preview, "#1874"),
    Ball8(R.drawable.launcher_8ball_preview, "8ball");

    override val title: TranslatableString
        get() = TranslatableString.PlainString(titleText)

    val launcherName: String
        get() = "${App.instance.packageName}.${this.name}LauncherAlias"


    companion object {
        private val map = values().associateBy(AppIcon::name)

        fun fromString(type: String?): AppIcon? = map[type]
    }
}