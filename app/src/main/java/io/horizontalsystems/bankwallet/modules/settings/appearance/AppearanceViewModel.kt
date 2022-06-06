package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.ui.compose.Select

class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService
) : ViewModel() {
    private var launchScreenOptions = getLaunchScreenOptions()

    var uiState by mutableStateOf(
        AppearanceUIState(
            launchScreenOptions = launchScreenOptions
        )
    )

    fun onLaunchPageSelect(launchPage: LaunchPage) {
        launchScreenService.selectLaunchPage(launchPage)
        launchScreenOptions = getLaunchScreenOptions()
        emitState()
    }

    private fun emitState() {
        uiState = AppearanceUIState(
            launchScreenOptions = launchScreenOptions
        )
    }

    private fun getLaunchScreenOptions(): Select<LaunchPage> {
        return Select(launchScreenService.selectedOption, launchScreenService.options)
    }

}

data class AppearanceUIState(
    val launchScreenOptions: Select<LaunchPage>
)
