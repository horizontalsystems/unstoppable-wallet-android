package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R

object CoinModule {

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.Coin_Tab_Overview),
        Details(R.string.Coin_Tab_Details),
        Market(R.string.Coin_Tab_Market),
//        Tweets(R.string.Coin_Tab_Tweets);
    }
}
