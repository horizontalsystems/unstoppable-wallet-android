package io.horizontalsystems.bankwallet.modules.coin.investments

import javax.annotation.concurrent.Immutable

object CoinInvestmentsModule {

    @Immutable
    data class ViewItem(
        val amount: String,
        val info: String,
        val fundViewItems: List<FundViewItem>
    )

    @Immutable
    data class FundViewItem(
        val name: String,
        val logoUrl: String,
        val isLead: Boolean,
        val url: String
    )
}
