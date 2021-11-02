package io.horizontalsystems.bankwallet.entities

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

enum class LaunchPage(@StringRes val titleResId: Int) {
    Auto(R.string.SettingsLaunchScreen_Auto),
    Balance(R.string.SettingsLaunchScreen_Balance),
    Market(R.string.SettingsLaunchScreen_MarketOverview),
    Watchlist(R.string.SettingsLaunchScreen_Watchlist);

    val icon: Int
        get() = when (this) {
            Auto -> R.drawable.ic_settings_20
            Balance -> R.drawable.ic_wallet_20
            Market -> R.drawable.ic_market_20
            Watchlist -> R.drawable.ic_star_20
        }

    companion object {
        private val map = values().associateBy(LaunchPage::name)

        fun fromString(type: String?): LaunchPage? = map[type]
    }
}
