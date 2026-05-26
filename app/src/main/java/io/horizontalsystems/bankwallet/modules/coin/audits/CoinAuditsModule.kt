package io.horizontalsystems.bankwallet.modules.coin.audits

import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.serialization.Serializable
import javax.annotation.concurrent.Immutable

object CoinAuditsModule {

    @Immutable
    data class AuditorViewItem(
        val name: String,
        val logoUrl: String,
        val auditViewItems: List<AuditViewItem>
    )

    @Immutable
    data class AuditViewItem(
        val date: String?,
        val name: String,
        val issues: TranslatableString,
        val reportUrl: String?
    )

    data class UiState(
        val auditors: List<AuditorViewItem>
    )

    @Serializable
    data class AuditParcelable(
        val date: String?,
        val name: String?,
        val auditUrl: String?,
        val techIssues: Int,
        val partnerName: String?
    )
}
