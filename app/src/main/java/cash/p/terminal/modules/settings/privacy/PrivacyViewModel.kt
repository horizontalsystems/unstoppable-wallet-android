package cash.p.terminal.modules.settings.privacy

import cash.p.terminal.core.ILocalStorage
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.horizontalsystems.core.ViewModelUiState

class PrivacyViewModel(private val localStorage: ILocalStorage) :
    ViewModelUiState<PrivacyUiState>() {
    override fun createState() = PrivacyUiState(
        uiStatsEnabled = localStorage.shareCrashDataEnabled
    )

    fun toggleCrashData(enabled: Boolean) {
        localStorage.shareCrashDataEnabled = enabled
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
        emitState()
    }
}

data class PrivacyUiState(val uiStatsEnabled: Boolean)
