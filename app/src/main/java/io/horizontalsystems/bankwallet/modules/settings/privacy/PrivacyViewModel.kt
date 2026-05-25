package io.horizontalsystems.bankwallet.modules.settings.privacy

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.core.stats.StatsManager
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val statsManager: StatsManager,
    private val appConfig: AppConfigProvider,
) : ViewModelUiState<PrivacyUiState>() {
    private var uiStatsEnabled = statsManager.uiStatsEnabledFlow.value
    private val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    val nymVpnLink by lazy { appConfig.nymVpnLink }

    init {
        viewModelScope.launch {
            statsManager.uiStatsEnabledFlow.collect {
                uiStatsEnabled = it
                emitState()
            }
        }
    }

    override fun createState() = PrivacyUiState(
        uiStatsEnabled = uiStatsEnabled,
        currentYear = currentYear
    )

    fun toggleUiStats(enabled: Boolean) {
        statsManager.toggleUiStats(enabled)
    }
}

data class PrivacyUiState(
    val uiStatsEnabled: Boolean,
    val currentYear: Int
)
