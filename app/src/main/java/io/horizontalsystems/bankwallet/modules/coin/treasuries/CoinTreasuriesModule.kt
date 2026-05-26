package io.horizontalsystems.bankwallet.modules.coin.treasuries

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle

object CoinTreasuriesModule {

    @Immutable
    data class CoinTreasuriesData(
        val treasuryTypeSelect: Select<TreasuryTypeFilter>,
        val sortDescending: Boolean,
        val coinTreasuries: List<CoinTreasuryItem>
    )

    @Immutable
    data class CoinTreasuryItem(
        val fund: String,
        val fundLogoUrl: String,
        val country: String,
        val amount: String,
        val amountInCurrency: String
    )

    enum class TreasuryTypeFilter : WithTranslatableTitle {
        All, Public, Private, ETF;

        override val title: TranslatableString
            get() = when (this) {
                All -> TranslatableString.ResString(R.string.MarketGlobalMetrics_ChainSelectorAll)
                else -> TranslatableString.PlainString(name)
            }
    }

    sealed class SelectorDialogState {
        object Closed : SelectorDialogState()
        class Opened(val select: Select<TreasuryTypeFilter>) : SelectorDialogState()
    }
}
