package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.annotations.SerializedName
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
                App.balanceViewTypeManager,
                App.localStorage,
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
    Ball8(R.drawable.launcher_8ball_preview, "8ball"),
    Pepe(R.drawable.launcher_pepe_preview, "Pepe"),
    Ivfun(R.drawable.launcher_ivfun_preview, "Ivfun"),
    Doge(R.drawable.launcher_doge_preview, "Doge"),
    Gigachad(R.drawable.launcher_gigachad_preview, "Gigachad"),
    Plflag(R.drawable.launcher_plflag_preview, "Plflag"),
    Yeschad(R.drawable.launcher_yeschad_preview, "Yeschad");

    override val title: TranslatableString
        get() = TranslatableString.PlainString(titleText)

    val launcherName: String
        get() = "${App.instance.packageName}.${this.name}LauncherAlias"


    companion object {
        private val map = values().associateBy(AppIcon::name)
        private val titleMap = values().associateBy(AppIcon::titleText)

        fun fromString(type: String?): AppIcon? = map[type]
        fun fromTitle(title: String?): AppIcon? = titleMap[title]
    }
}

enum class PriceChangeInterval(val raw: String, override val title: TranslatableString): WithTranslatableTitle {
    @SerializedName("hour_24")
    LAST_24H("hour_24", TranslatableString.ResString(R.string.Market_PriceChange_24H)),
    @SerializedName("midnight_utc")
    FROM_UTC_MIDNIGHT("midnight_utc", TranslatableString.ResString(R.string.Market_PriceChange_Utc));

    companion object {
        fun fromRaw(raw: String): PriceChangeInterval? {
            return entries.find { it.raw == raw }
        }
    }
}