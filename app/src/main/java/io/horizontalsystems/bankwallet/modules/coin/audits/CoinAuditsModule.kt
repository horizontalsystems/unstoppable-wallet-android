package io.horizontalsystems.bankwallet.modules.coin.audits

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.parcelize.Parcelize
import javax.annotation.concurrent.Immutable

object CoinAuditsModule {
    @Suppress("UNCHECKED_CAST")
    class Factory(private val audits: List<AuditParcelable>) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoinAuditsViewModel(audits) as T
        }
    }

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

    @Parcelize
    data class AuditParcelable(
        val date: String,
        val name: String,
        val auditUrl: String,
        val techIssues: Int,
        val partnerName: String
    ) : Parcelable
}
