package cash.p.terminal.modules.settings.appearance

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.core.managers.BaseTokenManager
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.modules.balance.BalanceViewType
import cash.p.terminal.modules.balance.BalanceViewTypeManager
import cash.p.terminal.modules.theme.ThemeService
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.ui.compose.SelectOptional
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow


class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService,
    private val appIconService: AppIconService,
    private val themeService: ThemeService,
    private val baseTokenManager: BaseTokenManager,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val localStorage: ILocalStorage,
    private val languageManager: LanguageManager,
    private val currencyManager: CurrencyManager,
) : ViewModelUiState<AppearanceUIState>() {
    private var launchScreenOptions = launchScreenService.optionsFlow.value
    private var appIconOptions = appIconService.optionsFlow.value
    private var themeOptions = themeService.optionsFlow.value
    private var baseTokenOptions = buildBaseTokenSelect(baseTokenManager.baseTokenFlow.value)
    private var marketsTabEnabled = localStorage.marketsTabEnabled
    private var balanceViewTypeOptions =
        buildBalanceViewTypeSelect(balanceViewTypeManager.balanceViewTypeFlow.value)
    private val currentLanguageDisplayName: String
        get() = languageManager.currentLanguageName

    private val baseCurrencyCode: String
        get() = currencyManager.baseCurrency.code

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
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                emitState()
            }
        }
    }

    override fun createState() = AppearanceUIState(
        launchScreenOptions = launchScreenOptions,
        appIconOptions = appIconOptions,
        themeOptions = themeOptions,
        baseTokenOptions = baseTokenOptions,
        balanceViewTypeOptions = balanceViewTypeOptions,
        marketsTabEnabled = marketsTabEnabled,
        currentLanguage = currentLanguageDisplayName,
        baseCurrencyCode = baseCurrencyCode
    )

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
    val currentLanguage: String,
    val baseCurrencyCode: String,
)
