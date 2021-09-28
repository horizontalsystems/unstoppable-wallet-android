package io.horizontalsystems.bankwallet.modules.coin.audits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import java.util.*

object CoinAuditsModule {
    class Factory(private val coinType: CoinType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CoinAuditsViewModel(
                App.xRateManager,
                coinType
            ) as T
        }
    }
}

sealed class CoinAuditItem {
    class Header(val name: String) : CoinAuditItem()
    class Report(
        val name: String,
        val date: Date,
        val issues: Int = 0,
        val link: String?,
        val position: ListPosition
    ) : CoinAuditItem()
}
