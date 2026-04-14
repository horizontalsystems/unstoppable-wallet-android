package com.quantum.wallet.bankwallet.modules.balance

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.WithTranslatableTitle

enum class BalanceViewType(@StringRes val titleResId: Int, @StringRes val subtitleResId: Int) :
    WithTranslatableTitle {
    @SerializedName("coin")
    CoinThenFiat(R.string.BalanceViewType_CoinValue, R.string.BalanceViewType_FiatValue),

    @SerializedName("currency")
    FiatThenCoin(R.string.BalanceViewType_FiatValue, R.string.BalanceViewType_CoinValue);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)
}
