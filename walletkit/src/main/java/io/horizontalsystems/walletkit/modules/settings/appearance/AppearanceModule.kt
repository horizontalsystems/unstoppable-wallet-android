package io.horizontalsystems.walletkit.modules.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.LaunchPage
import io.horizontalsystems.walletkit.modules.balance.BalanceViewType
import io.horizontalsystems.walletkit.modules.theme.ThemeService
import io.horizontalsystems.walletkit.modules.theme.ThemeType
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle

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
                App.languageManager,
                App.currencyManager,
            ) as T
        }
    }

}

/**
 * What each setting of the Appearance page holds until the user changes it.
 * The storage fallbacks and the page's Reset both read these, so they cannot
 * disagree.
 */
object AppearanceDefaults {
    val theme = ThemeType.System
    val baseCurrencyCode = "USD"
    val launchPage = LaunchPage.Auto
    val marketsTabEnabled = true
    val priceChangeInterval = PriceChangeInterval.LAST_24H
    val balanceTabButtonsEnabled = true
    val chartButtonEnabled = true
    val amountRoundingEnabled = true
    val balanceViewType = BalanceViewType.CoinThenFiat
    val recentlySentEnabled = false
}

/**
 * @param titleText identifies the icon in backups and stats, so it must not
 * change once released.
 * @param displayName what the user sees, when it differs from [titleText].
 */
enum class AppIcon(
    val icon: Int,
    val titleText: String,
    val isDeprecated: Boolean = false,
    private val displayName: String = titleText,
) : WithTranslatableTitle {
    Main(R.drawable.launcher_main_preview, "Main"),
    Dark(R.drawable.launcher_dark_preview, "Dark", displayName = "Orange"),
    Mono(R.drawable.launcher_mono_preview, "Mono"),
    Ball8(R.drawable.launcher_8ball_preview, "8ball", displayName = "8Ball"),

    // A retired icon leaves the picker, and a user who had it gets Main at
    // the next launch. Its manifest alias has to outlive that by two
    // releases: an update that drops the alias of the icon still selected
    // leaves the app without a launcher entry.

    // Retired in 0.54, remove with their aliases in 0.56
    Monero(R.drawable.launcher_monero_preview, "monero", true),
    ZCash(R.drawable.launcher_zcash_preview, "ZCash", true),
    Pepe(R.drawable.launcher_pepe_preview, "Pepe", true),
    Doge(R.drawable.launcher_doge_preview, "Doge", true),
    Punk(R.drawable.launcher_punk_preview, "Punk", true),
    Ape(R.drawable.launcher_ape_preview, "#1874", true),
    Plflag(R.drawable.launcher_plflag_preview, "Plflag", true),
    Sinwar(R.drawable.launcher_sinwar_preview, "Sinwar", true),
    Yeschad(R.drawable.launcher_yeschad_preview, "Yeschad", true),
    Gigachad(R.drawable.launcher_gigachad_preview, "Gigachad", true);

    override val title: TranslatableString
        get() = TranslatableString.PlainString(displayName)

    val launcherName: String
        get() = "${App.instance.packageName}.${this.name}LauncherAlias"


    companion object {
        private val map = values().associateBy(AppIcon::name)
        private val titleMap = values().associateBy(AppIcon::titleText)

        fun getActiveIcons(): List<AppIcon> = entries.filter { !it.isDeprecated }
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