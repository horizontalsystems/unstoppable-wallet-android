package cash.p.terminal.wallet.balance

import androidx.annotation.StringRes
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.WithTranslatableTitle
import cash.p.terminal.wallet.R
import com.google.gson.annotations.SerializedName

enum class BalanceViewType(@StringRes val titleResId: Int, @StringRes val subtitleResId: Int) :
    WithTranslatableTitle {
    @SerializedName("coin")
    CoinThenFiat(R.string.BalanceViewType_CoinValue, R.string.BalanceViewType_FiatValue),

    @SerializedName("currency")
    FiatThenCoin(R.string.BalanceViewType_FiatValue, R.string.BalanceViewType_CoinValue);

    override val title: TranslatableString
        get() = TranslatableString.ResString(titleResId)
}
