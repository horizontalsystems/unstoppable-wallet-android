package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.stats.statValue
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewType
import io.horizontalsystems.bankwallet.modules.balance.BalanceViewTypeManager
import io.horizontalsystems.bankwallet.modules.theme.ThemeService
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.Select
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

        stat(page = StatPage.Appearance, event = StatEvent.SelectLaunchScreen(launchPage.statValue))
    }

    fun onEnterAppIcon(enabledAppIcon: AppIcon) {
        appIconService.setAppIcon(enabledAppIcon)

        stat(page = StatPage.Appearance, event = StatEvent.SelectAppIcon(enabledAppIcon.titleText.lowercase()))
    }

    fun onEnterTheme(themeType: ThemeType) {
        themeService.setThemeType(themeType)

        stat(page = StatPage.Appearance, event = StatEvent.SelectTheme(themeType.statValue))
    }

    fun onEnterBalanceViewType(viewType: BalanceViewType) {
        balanceViewTypeManager.setViewType(viewType)

        stat(page = StatPage.Appearance, event = StatEvent.SelectBalanceValue(viewType.statValue))
    }

    fun onSetMarketTabsHidden(hidden: Boolean) {
        if (hidden && (launchScreenOptions.selected == LaunchPage.Market || launchScreenOptions.selected == LaunchPage.Watchlist)) {
            launchScreenService.setLaunchScreen(LaunchPage.Auto)
        }
        localStorage.marketsTabEnabled = !hidden

        marketsTabHidden = hidden
        emitState()

        stat(page = StatPage.Appearance, event = StatEvent.ShowMarketsTab(shown = !hidden))
    }

    fun onSetBalanceTabButtonsHidden(hidden: Boolean) {
        localStorage.balanceTabButtonsEnabled = !hidden

        balanceTabButtonsHidden = hidden
        emitState()

        stat(page = StatPage.Appearance, event = StatEvent.HideBalanceButtons(shown = !hidden))
    }

    fun onSetPriceChangeInterval(priceChangeInterval: PriceChangeInterval) {
        localStorage.priceChangeInterval = priceChangeInterval

        this.priceChangeInterval = priceChangeInterval
        this.priceChangeIntervalOptions = buildPriceChangeIntervalSelect(priceChangeInterval)
        emitState()

        stat(page = StatPage.Appearance, event = StatEvent.SwitchPriceChangeMode(priceChangeInterval.statValue))
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
