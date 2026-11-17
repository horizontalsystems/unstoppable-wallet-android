package io.horizontalsystems.walletkit.modules.balance

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle

enum class BalanceViewType(@StringRes val titleResId: Int) : WithTranslatableTitle {
    @SerializedName("coin")
    CoinThenFiat(R.string.BalanceViewType_CoinFiat),

    @SerializedName("currency")
    FiatThenCoin(R.string.BalanceViewType_FiatCoin);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)
}
