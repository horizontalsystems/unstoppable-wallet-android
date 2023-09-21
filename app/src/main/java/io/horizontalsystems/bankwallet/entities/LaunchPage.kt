package io.horizontalsystems.bankwallet.entities

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

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
        private val map = values().associateBy(LaunchPage::name)

        fun fromString(type: String?): LaunchPage? = map[type]
    }
}
