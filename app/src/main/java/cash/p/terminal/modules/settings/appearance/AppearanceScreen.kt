package cash.p.terminal.modules.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.modules.theme.ThemeType
import cash.p.terminal.ui.compose.components.AlertGroup
import cash.p.terminal.ui_compose.Select
import cash.p.terminal.wallet.balance.BalanceViewType
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HeaderText
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.HsSwitch
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun AppearanceScreen(
    uiState: AppearanceUIState,
    onThemeSelect: (ThemeType) -> Unit,
    onLaunchScreenSelect: (LaunchPage) -> Unit,
    onBalanceViewTypeSelect: (BalanceViewType) -> Unit,
    onPriceChangeIntervalSelect: (PriceChangeInterval) -> Unit,
    onMarketTabsHiddenChange: (Boolean) -> Unit,
    onBalanceTabButtonsHiddenChange: (Boolean) -> Unit,
    onAppIconClick: () -> Unit,
    onClose: () -> Unit
) {
    var openThemeSelector by rememberSaveable { mutableStateOf(false) }
    var openLaunchPageSelector by rememberSaveable { mutableStateOf(false) }
    var openBalanceValueSelector by rememberSaveable { mutableStateOf(false) }
    var openPriceChangeIntervalSelector by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_Appearance),
                navigationIcon = {
                    HsBackButton(onClick = onClose)
                },
                menuItems = listOf()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            VSpacer(height = 12.dp)
            CellUniversalLawrenceSection(
                listOf {
                    MenuItemWithDialog(
                        R.string.Settings_Theme,
                        value = uiState.selectedTheme.title.getString(),
                        onClick = { openThemeSelector = true }
                    )
                }
            )

            VSpacer(24.dp)

            HeaderText(text = stringResource(id = R.string.Appearance_MarketsTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            body_leah(
                                text = stringResource(id = R.string.Appearance_HideMarketsTab),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            )
                            HsSwitch(
                                checked = uiState.marketsTabHidden,
                                onCheckedChange = onMarketTabsHiddenChange
                            )
                        }
                    },
                    {
                        MenuItemWithDialog(
                            R.string.Appearance_PriceChangeInterval,
                            value = uiState.priceChangeInterval.title.getString(),
                            onClick = { openPriceChangeIntervalSelector = true }
                        )
                    }
                )
            )

            AnimatedVisibility(visible = !uiState.marketsTabHidden) {
                Column {
                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            MenuItemWithDialog(
                                R.string.Settings_LaunchScreen,
                                value = uiState.selectedLaunchScreen.title.getString(),
                                onClick = { openLaunchPageSelector = true }
                            )
                        }
                    )
                }
            }

            VSpacer(24.dp)
            HeaderText(text = stringResource(id = R.string.Appearance_BalanceTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            body_leah(
                                text = stringResource(id = R.string.Appearance_HideBalanceTabButtons),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            )
                            HsSwitch(
                                checked = uiState.balanceTabButtonsHidden,
                                onCheckedChange = onBalanceTabButtonsHiddenChange
                            )
                        }
                    },
                    {
                        MenuItemWithDialog(
                            R.string.Appearance_BalanceValue,
                            value = uiState.selectedBalanceViewType.title.getString(),
                            onClick = { openBalanceValueSelector = true }
                        )
                    }
                )
            )

            VSpacer(22.dp)
            CellUniversalLawrenceSection(
                listOf {
                    RowUniversal(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onAppIconClick
                    ) {
                        body_leah(
                            text = stringResource(R.string.Appearance_AppIcon),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = ComposeAppTheme.colors.grey
                        )
                    }
                }
            )

            VSpacer(32.dp)
        }
    }

    if (openThemeSelector) {
        AlertGroup(
            R.string.Settings_Theme,
            uiState.themeOptions,
            { selected ->
                onThemeSelect(selected)
                openThemeSelector = false
            },
            { openThemeSelector = false }
        )
    }
    if (openLaunchPageSelector) {
        AlertGroup(
            R.string.Settings_LaunchScreen,
            uiState.launchScreenOptions,
            { selected ->
                onLaunchScreenSelect(selected)
                openLaunchPageSelector = false
            },
            { openLaunchPageSelector = false }
        )
    }
    if (openBalanceValueSelector) {
        AlertGroup(
            R.string.Appearance_BalanceValue,
            uiState.balanceViewTypeOptions,
            { selected ->
                onBalanceViewTypeSelect(selected)
                openBalanceValueSelector = false
            },
            { openBalanceValueSelector = false }
        )
    }
    if (openPriceChangeIntervalSelector) {
        AlertGroup(
            R.string.Appearance_PriceChangeInterval,
            uiState.priceChangeIntervalOptions,
            { selected ->
                onPriceChangeIntervalSelect(selected)
                openPriceChangeIntervalSelector = false
            },
            { openPriceChangeIntervalSelector = false }
        )
    }
}

@Composable
internal fun MenuItemWithDialog(
    @StringRes title: Int,
    value: String,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = stringResource(title),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        subhead1_grey(
            text = value,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_down_arrow_20),
            contentDescription = null
        )
    }
}

@Preview
@Composable
private fun AppearanceScreenPreview() {
    ComposeAppTheme {
        AppearanceScreen(
            uiState = AppearanceUIState(
                launchScreenOptions = Select(LaunchPage.Auto, LaunchPage.entries),
                appIconOptions = Select(AppIcon.Main, AppIcon.entries),
                themeOptions = Select(ThemeType.Dark, ThemeType.entries),
                balanceViewTypeOptions = Select(BalanceViewType.CoinThenFiat, BalanceViewType.entries),
                marketsTabHidden = false,
                balanceTabButtonsHidden = false,
                selectedTheme = ThemeType.Dark,
                selectedLaunchScreen = LaunchPage.Auto,
                selectedBalanceViewType = BalanceViewType.CoinThenFiat,
                priceChangeInterval = PriceChangeInterval.LAST_24H,
                priceChangeIntervalOptions = Select(PriceChangeInterval.LAST_24H, PriceChangeInterval.entries),
                pushNotificationsEnabled = false,
                isCalculatorModeEnabled = false,
            ),
            onThemeSelect = {},
            onLaunchScreenSelect = {},
            onBalanceViewTypeSelect = {},
            onPriceChangeIntervalSelect = {},
            onMarketTabsHiddenChange = {},
            onBalanceTabButtonsHiddenChange = {},
            onAppIconClick = {},
            onClose = {}
        )
    }
}
