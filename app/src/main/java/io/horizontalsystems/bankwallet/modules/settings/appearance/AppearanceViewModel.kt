package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.BaseTokenManager
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.SelectOptional
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch


class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val baseTokenManager: BaseTokenManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val localStorage: ILocalStorage
) : ViewModel() {
    private var launchScreenOptions = launchScreenService.optionsFlow.value
    private var appIconOptions = appIconService.optionsFlow.value
    private var themeOptions = themeService.optionsFlow.value
    private var baseTokenOptions = buildBaseTokenSelect(baseTokenManager.baseTokenFlow.value)
    private var marketsTabEnabled = localStorage.marketsTabEnabled
    private var balanceViewTypeOptions =
        buildBalanceViewTypeSelect(balanceViewTypeManager.balanceViewTypeFlow.value)

    var uiState by mutableStateOf(
        AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            appIconOptions = appIconOptions,
            themeOptions = themeOptions,
            baseTokenOptions = baseTokenOptions,
            balanceViewTypeOptions = balanceViewTypeOptions,
            marketsTabEnabled = marketsTabEnabled
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
            appIconService.optionsFlow
                .collect {
                    handleUpdatedAppIconOptions(it)
                }
        }
        viewModelScope.launch {
            themeService.optionsFlow
                .collect {
                    handleUpdatedThemeOptions(it)
                }
        }
        viewModelScope.launch {
            baseTokenManager.baseTokenFlow
                .collect { baseToken ->
                    handleUpdatedBaseToken(buildBaseTokenSelect(baseToken))
                }
        }
        viewModelScope.launch {
            balanceViewTypeManager.balanceViewTypeFlow
                .collect {
                    handleUpdatedBalanceViewType(buildBalanceViewTypeSelect(it))
                }
        }
    }

    private fun buildBaseTokenSelect(token: Token?): SelectOptional<Token> {
        return SelectOptional(token, baseTokenManager.tokens)
    }

    private fun buildBalanceViewTypeSelect(value: BalanceViewType): Select<BalanceViewType> {
        return Select(value, balanceViewTypeManager.viewTypes)
    }

    private fun handleUpdatedLaunchScreenOptions(launchScreenOptions: Select<LaunchPage>) {
        this.launchScreenOptions = launchScreenOptions
        emitState()
    }

    private fun handleUpdatedAppIconOptions(appIconOptions: Select<AppIcon>) {
        this.appIconOptions = appIconOptions
        emitState()
    }

    private fun handleUpdatedThemeOptions(themeOptions: Select<ThemeType>) {
        this.themeOptions = themeOptions
        emitState()
    }

    private fun handleUpdatedBalanceViewType(balanceViewTypeOptions: Select<BalanceViewType>) {
        this.balanceViewTypeOptions = balanceViewTypeOptions
        emitState()
    }

    private fun handleUpdatedBaseToken(baseTokenOptions: SelectOptional<Token>) {
        this.baseTokenOptions = baseTokenOptions
        emitState()
    }

    private fun emitState() {
        uiState = AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            appIconOptions = appIconOptions,
            themeOptions = themeOptions,
            baseTokenOptions = baseTokenOptions,
            balanceViewTypeOptions = balanceViewTypeOptions,
            marketsTabEnabled = marketsTabEnabled,
        )
    }

    fun onEnterLaunchPage(launchPage: LaunchPage) {
        launchScreenService.setLaunchScreen(launchPage)
    }

    fun onEnterAppIcon(enabledAppIcon: AppIcon) {
        appIconService.setAppIcon(enabledAppIcon)
    }

    fun onEnterTheme(themeType: ThemeType) {
        themeService.setThemeType(themeType)
    }

    fun onEnterBaseToken(token: Token) {
        baseTokenManager.setBaseToken(token)
    }

    fun onEnterBalanceViewType(viewType: BalanceViewType) {
        balanceViewTypeManager.setViewType(viewType)
    }

    fun onSetMarketTabsEnabled(enabled: Boolean) {
        if (enabled.not() && (launchScreenOptions.selected == LaunchPage.Market || launchScreenOptions.selected == LaunchPage.Watchlist)) {
            launchScreenService.setLaunchScreen(LaunchPage.Auto)
        }
        localStorage.marketsTabEnabled = enabled

        marketsTabEnabled = enabled
        emitState()
    }

}

data class AppearanceUIState(
    val launchScreenOptions: Select<LaunchPage>,
    val appIconOptions: Select<AppIcon>,
    val themeOptions: Select<ThemeType>,
    val baseTokenOptions: SelectOptional<Token>,
    val balanceViewTypeOptions: Select<BalanceViewType>,
    val marketsTabEnabled: Boolean,
)
