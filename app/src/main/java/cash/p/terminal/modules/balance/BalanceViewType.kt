package cash.p.terminal.modules.balance

import androidx.annotation.StringRes
import cash.p.terminal.R

enum class BalanceViewType(@StringRes val titleResId: Int, @StringRes val subtitleResId: Int) {
    CoinThenFiat(R.string.BalanceViewType_CoinValue, R.string.BalanceViewType_FiatValue),
    FiatThenCoin(R.string.BalanceViewType_FiatValue, R.string.BalanceViewType_CoinValue);
}
