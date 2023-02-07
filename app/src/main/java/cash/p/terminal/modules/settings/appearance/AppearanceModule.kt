package cash.p.terminal.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.WithTranslatableTitle

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
                App.localStorage
            ) as T
        }
    }

}

enum class AppIcon(val icon: Int, val titleText: String) : WithTranslatableTitle {
    Main(R.drawable.launcher_main_preview, "Main"),
    Dark(R.drawable.launcher_dark_preview, "Dark"),
    Mono(R.drawable.launcher_mono_preview, "Mono");

    override val title: TranslatableString
        get() = TranslatableString.PlainString(titleText)

    val launcherName: String
        get() = "${App.instance.packageName}.${this.name}LauncherAlias"


    companion object {
        private val map = values().associateBy(AppIcon::name)

        fun fromString(type: String?): AppIcon? = map[type]
    }
}