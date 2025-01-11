package cash.p.terminal.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import cash.p.terminal.R
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.WithTranslatableTitle

enum class LaunchPage(@StringRes val titleRes: Int, @DrawableRes val iconRes: Int):
    WithTranslatableTitle {
    @SerializedName("auto")
    Auto(R.string.SettingsLaunchScreen_Auto, R.drawable.ic_settings_20),
    @SerializedName("balance")
    Balance(R.string.SettingsLaunchScreen_Balance, R.drawable.ic_wallet_20),
    @SerializedName("market")
    Market(R.string.SettingsLaunchScreen_MarketOverview, R.drawable.ic_market_20),
    @SerializedName("watchlist")
    Watchlist(R.string.SettingsLaunchScreen_Watchlist, R.drawable.ic_star_20);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleRes)

    companion object {
        private val map = entries.associateBy(LaunchPage::name)

        fun fromString(type: String?): LaunchPage? = map[type]
    }
}
