package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.managers.BaseCoinManager
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.SelectOptional
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService,
    private val themeService: ThemeService,
    private val baseCoinManager: BaseCoinManager
) : ViewModel() {
    private var launchScreenOptions = launchScreenService.optionsFlow.value
    private var themeOptions = themeService.optionsFlow.value
    private var baseCoinOptions = buildBaseCoinSelect(baseCoinManager.baseCoinFlow.value)

    var uiState by mutableStateOf(
        AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            themeOptions = themeOptions,
            baseCoinOptions = baseCoinOptions,
        )
    )

    init {
        viewModelScope.launch {
            launchScreenService.optionsFlow
                .collect {
                    handleUpdatedLaunchScreenOptions(it)
                }
        }
        viewModelScope.launch {
            themeService.optionsFlow
                .collect {
                    handleUpdatedThemeOptions(it)
                }
        }
        viewModelScope.launch {
            baseCoinManager.baseCoinFlow
                .collect { baseCoin ->
                    handleUpdatedBaseCoin(buildBaseCoinSelect(baseCoin))
                }
        }
    }

    private fun buildBaseCoinSelect(platformCoin: PlatformCoin?): SelectOptional<PlatformCoin> {
        return SelectOptional(platformCoin, baseCoinManager.platformCoins)
    }

    private fun handleUpdatedLaunchScreenOptions(launchScreenOptions: Select<LaunchPage>) {
        this.launchScreenOptions = launchScreenOptions
        emitState()
    }

    private fun handleUpdatedThemeOptions(themeOptions: Select<ThemeType>) {
        this.themeOptions = themeOptions
        emitState()
    }

    private fun handleUpdatedBaseCoin(baseCoinOptions: SelectOptional<PlatformCoin>) {
        this.baseCoinOptions = baseCoinOptions
        emitState()
    }

    private fun emitState() {
        uiState = AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            themeOptions = themeOptions,
            baseCoinOptions = baseCoinOptions
        )
    }

    fun onEnterLaunchPage(launchPage: LaunchPage) {
        launchScreenService.setLaunchScreen(launchPage)
    }

    fun onEnterTheme(themeType: ThemeType) {
        themeService.setThemeType(themeType)
    }

    fun onEnterBaseCoin(platformCoin: PlatformCoin) {
        baseCoinManager.setBaseCoin(platformCoin)
    }
}

data class AppearanceUIState(
    val launchScreenOptions: Select<LaunchPage>,
    val themeOptions: Select<ThemeType>,
    val baseCoinOptions: SelectOptional<PlatformCoin>
)
