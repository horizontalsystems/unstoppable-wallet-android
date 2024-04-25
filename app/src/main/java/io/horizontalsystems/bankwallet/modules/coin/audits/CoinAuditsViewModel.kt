package io.horizontalsystems.bankwallet.modules.coin.audits

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsModule.AuditViewItem
import io.horizontalsystems.bankwallet.modules.coin.audits.CoinAuditsModule.AuditorViewItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
class CoinAuditsViewModel(
    audits: List<CoinAuditsModule.AuditParcelable>
) : ViewModel() {

    var uiState by mutableStateOf(CoinAuditsModule.UiState(emptyList()))

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val viewItems = CoinAuditsModule.UiState(auditorViewItems(audits))
            withContext(Dispatchers.Main) {
                uiState = viewItems
            }
        }
    }

    private fun auditorViewItems(audits: List<CoinAuditsModule.AuditParcelable>): List<AuditorViewItem> {
        val groupedByAuditor = audits.groupBy { it.partnerName }
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val auditorViewItems = mutableListOf<AuditorViewItem>()
        groupedByAuditor.forEach { (auditor, reports) ->
            auditorViewItems.add(
                AuditorViewItem(
                    name = auditor ?: "",
                    logoUrl = auditor?.let { logoUrl(it) } ?: "",
                    auditViewItems = reports.map { report ->
                        AuditViewItem(
                            date = report.date?.let { formatter.parse(it) }?.let { date ->
                                DateHelper.formatDate(date, "MMM dd, yyyy")
                            },
                            name = report.name ?: "",
                            issues = TranslatableString.ResString(
                                R.string.CoinPage_Audits_Issues,
                                report.techIssues
                            ),
                            reportUrl = report.auditUrl
                        )
                    }
                ))
        }
        return auditorViewItems
    }

    private fun logoUrl(name: String): String =
        "https://cdn.blocksdecoded.com/auditor-icons/$name@3x.png"

}
