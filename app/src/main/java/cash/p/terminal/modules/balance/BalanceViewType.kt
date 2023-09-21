package cash.p.terminal.modules.balance

import androidx.annotation.StringRes
import com.google.gson.annotations.SerializedName
import cash.p.terminal.R

enum class BalanceViewType(@StringRes val titleResId: Int, @StringRes val subtitleResId: Int) {
    @SerializedName("coin")
    CoinThenFiat(R.string.BalanceViewType_CoinValue, R.string.BalanceViewType_FiatValue),
    @SerializedName("currency")
    FiatThenCoin(R.string.BalanceViewType_FiatValue, R.string.BalanceViewType_CoinValue);
}
