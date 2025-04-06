package cash.p.terminal.modules.settings.appearance

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<AppearanceUIState>() {
    private var launchScreenOptions = launchScreenService.optionsFlow.value
    private var appIconOptions = appIconService.optionsFlow.value
    private var themeOptions = themeService.optionsFlow.value
    private var marketsTabHidden = !localStorage.marketsTabEnabled
    private var balanceTabButtonsHidden = !localStorage.balanceTabButtonsEnabled
    private var balanceViewTypeOptions = buildBalanceViewTypeSelect(balanceViewTypeManager.balanceViewTypeFlow.value)
    private var priceChangeInterval = localStorage.priceChangeInterval
    private var priceChangeIntervalOptions = buildPriceChangeIntervalSelect(priceChangeInterval)

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
            balanceViewTypeManager.balanceViewTypeFlow
                .collect {
                    handleUpdatedBalanceViewType(buildBalanceViewTypeSelect(it))
                }
        }
    }

    override fun createState() = AppearanceUIState(
        launchScreenOptions = launchScreenOptions,
        appIconOptions = appIconOptions,
        themeOptions = themeOptions,
        balanceViewTypeOptions = balanceViewTypeOptions,
        marketsTabHidden = marketsTabHidden,
        balanceTabButtonsHidden = balanceTabButtonsHidden,
        selectedTheme = themeService.selectedTheme,
        selectedLaunchScreen = launchScreenService.selectedLaunchScreen,
        selectedBalanceViewType = balanceViewTypeManager.balanceViewType,
        priceChangeInterval = priceChangeInterval,
        priceChangeIntervalOptions = priceChangeIntervalOptions
    )

    private fun buildBalanceViewTypeSelect(value: BalanceViewType): Select<BalanceViewType> {
        return Select(value, balanceViewTypeManager.viewTypes)
    }

    private fun buildPriceChangeIntervalSelect(value: PriceChangeInterval): Select<PriceChangeInterval> {
        return Select(value, PriceChangeInterval.entries)
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

    fun onEnterLaunchPage(launchPage: LaunchPage) {
        launchScreenService.setLaunchScreen(launchPage)
    }

    fun onEnterAppIcon(enabledAppIcon: AppIcon) {
        appIconService.setAppIcon(enabledAppIcon)
    }

    fun onEnterTheme(themeType: ThemeType) {
        themeService.setThemeType(themeType)
    }

    fun onEnterBalanceViewType(viewType: BalanceViewType) {
        balanceViewTypeManager.setViewType(viewType)
    }

    fun onSetMarketTabsHidden(hidden: Boolean) {
        if (hidden && (launchScreenOptions.selected == LaunchPage.Market || launchScreenOptions.selected == LaunchPage.Watchlist)) {
            launchScreenService.setLaunchScreen(LaunchPage.Auto)
        }
        localStorage.marketsTabEnabled = !hidden

        marketsTabHidden = hidden
        emitState()
    }

    fun onSetBalanceTabButtonsHidden(hidden: Boolean) {
        localStorage.balanceTabButtonsEnabled = !hidden

        balanceTabButtonsHidden = hidden
        emitState()
    }

    fun onSetPriceChangeInterval(priceChangeInterval: PriceChangeInterval) {
        localStorage.priceChangeInterval = priceChangeInterval

        this.priceChangeInterval = priceChangeInterval
        this.priceChangeIntervalOptions = buildPriceChangeIntervalSelect(priceChangeInterval)
        emitState()
    }

}

data class AppearanceUIState(
    val launchScreenOptions: Select<LaunchPage>,
    val appIconOptions: Select<AppIcon>,
    val themeOptions: Select<ThemeType>,
    val balanceViewTypeOptions: Select<BalanceViewType>,
    val marketsTabHidden: Boolean,
    val balanceTabButtonsHidden: Boolean,
    val selectedTheme: ThemeType,
    val selectedLaunchScreen: LaunchPage,
    val selectedBalanceViewType: BalanceViewType,
    val priceChangeInterval: PriceChangeInterval,
    val priceChangeIntervalOptions: Select<PriceChangeInterval>
)
