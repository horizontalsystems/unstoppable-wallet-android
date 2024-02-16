package io.horizontalsystems.bankwallet.modules.coin.detectors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.modules.coin.detectors.DetectorsModule.IssueViewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetectorsViewModel(
    private val title: String,
    private val detectors: List<IssueParcelable>
) : ViewModel() {

    var coreIssues = emptyList<IssueViewItem>()
    var generalIssues = emptyList<IssueViewItem>()
    var uiState by mutableStateOf(
        DetectorsModule.UiState(
            title,
            coreIssues,
            generalIssues
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val coreIssueList = mutableListOf<IssueParcelable>()
            val generalIssueList = mutableListOf<IssueParcelable>()
            detectors.forEach { issue ->
                if (issue.issue == "general") {
                    generalIssueList.add(issue)
                } else {
                    coreIssueList.add(issue)
                }
            }

            coreIssues = sortIssuesByImpact(coreIssueList)
            generalIssues = sortIssuesByImpact(generalIssueList)

            withContext(Dispatchers.Main) {
                sync()
            }
        }
    }

    fun toggleExpandGeneral(id: Int) {
        generalIssues = generalIssues.map {
            if (it.id == id) {
                it.copy(expanded = !it.expanded)
            } else {
                it
            }
        }
        uiState = uiState.copy(
            generalIssues = generalIssues
        )
    }

    fun toggleExpandCore(id: Int) {
        coreIssues = coreIssues.map {
            if (it.id == id) {
                it.copy(expanded = !it.expanded)
            } else {
                it
            }
        }
        uiState = uiState.copy(
            coreIssues = coreIssues
        )
    }

    private fun sync() {
        uiState = DetectorsModule.UiState(
            title,
            coreIssues,
            generalIssues
        )
    }

    private fun sortIssuesByImpact(issues: List<IssueParcelable>): List<IssueViewItem> {
        return issues.sortedWith(compareByDescending { issue ->
            issue.issues?.getOrNull(0)?.impact?.let { impact ->
                when (impact) {
                    "Critical" -> 5
                    "High" -> 4
                    "Low" -> 3
                    "Informational" -> 2
                    else -> 1
                }
            } ?: 0
        }).map { IssueViewItem(it.description.hashCode(), it) }
    }

}