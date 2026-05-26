package io.horizontalsystems.bankwallet.modules.coin.reports

import javax.annotation.concurrent.Immutable

object CoinReportsModule {

    @Immutable
    data class ReportViewItem(
        val author: String,
        val title: String,
        val body: String,
        val date: String,
        val url: String
    )
}
