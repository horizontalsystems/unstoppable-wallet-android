package io.horizontalsystems.bankwallet.modules.coin.detectors

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.coin.detectors.DetectorsModule.IssueViewItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetectorsViewModel(
    private val title: String,
    private val detectors: List<IssueParcelable>
) : ViewModelUiState<DetectorsModule.UiState>() {

    var coreIssues = emptyList<IssueViewItem>()
    var generalIssues = emptyList<IssueViewItem>()

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

            emitState()
        }
    }

    override fun createState() = DetectorsModule.UiState(
        title = title,
        coreIssues = coreIssues,
        generalIssues = generalIssues
    )

    fun toggleExpandGeneral(id: Int) {
        generalIssues = generalIssues.map {
            if (it.id == id) {
                it.copy(expanded = !it.expanded)
            } else {
                it
            }
        }
        emitState()
    }

    fun toggleExpandCore(id: Int) {
        coreIssues = coreIssues.map {
            if (it.id == id) {
                it.copy(expanded = !it.expanded)
            } else {
                it
            }
        }
        emitState()
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