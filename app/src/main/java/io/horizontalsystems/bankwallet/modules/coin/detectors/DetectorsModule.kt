package io.horizontalsystems.bankwallet.modules.coin.detectors

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import kotlinx.parcelize.Parcelize

object DetectorsModule {
    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val title: String,
        private val detectors: List<IssueParcelable>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetectorsViewModel(title, detectors) as T
        }
    }

    data class UiState(
        val title: String,
        val coreIssues: List<IssueViewItem>,
        val generalIssues: List<IssueViewItem>
    )

    enum class DetectorsTab(@StringRes val titleResId: Int) {
        Token(R.string.Detectors_TokenDetectors),
        General(R.string.Detectors_GeneralDetectors);
    }

    data class IssueViewItem(
        val id: Int,
        val issue: IssueParcelable,
        val expanded: Boolean = false
    )
}

@Parcelize
data class IssueParcelable(
    val issue: String,
    val title: String? = null,
    val description: String,
    val issues: List<IssueItemParcelable>? = null,
) : Parcelable

@Parcelize
data class IssueItemParcelable(
    val impact: String,
    val confidence: String? = null,
    val description: String,
) : Parcelable
