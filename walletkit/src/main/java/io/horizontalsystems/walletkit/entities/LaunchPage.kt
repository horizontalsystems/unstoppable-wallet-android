package io.horizontalsystems.walletkit.entities

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle

enum class LaunchPage(@StringRes val titleRes: Int) : WithTranslatableTitle {
    @SerializedName("auto")
    Auto(R.string.SettingsLaunchScreen_Auto),
    @SerializedName("balance")
    Balance(R.string.SettingsLaunchScreen_Balance),
    @SerializedName("market")
    Market(R.string.SettingsLaunchScreen_MarketOverview),
    @SerializedName("watchlist")
    Watchlist(R.string.SettingsLaunchScreen_Watchlist);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleRes)

    companion object {
        private val map = entries.associateBy(LaunchPage::name)

        fun fromString(type: String?): LaunchPage? = map[type]
    }
}
