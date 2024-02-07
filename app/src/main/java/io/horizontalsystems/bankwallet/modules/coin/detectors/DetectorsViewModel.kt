package io.horizontalsystems.bankwallet.modules.coin.detectors

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetectorsViewModel(
    private val title: String,
    private val detectors: List<IssueParcelable>
) : ViewModel() {

    var coreIssues = emptyList<IssueParcelable>()
    var generalIssues = emptyList<IssueParcelable>()
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

    fun sync() {
        uiState = DetectorsModule.UiState(
            title,
            coreIssues,
            generalIssues
        )
    }

    private fun sortIssuesByImpact(issues: List<IssueParcelable>): List<IssueParcelable> {
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
        })
    }

}