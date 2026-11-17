package io.horizontalsystems.walletkit.modules.settings.appearance

import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSButton
import io.horizontalsystems.designkit.components.HSDropdownButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.uiv3.TestTags
import androidx.compose.ui.platform.testTag
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.basecurrency.BaseCurrencySettingsPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.settings.language.LanguageSettingsPage
import io.horizontalsystems.walletkit.ui.compose.Select
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.ui.compose.components.HeaderText
import io.horizontalsystems.walletkit.ui.compose.components.MenuItem
import io.horizontalsystems.walletkit.ui.compose.components.TextImportantWarning
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_orange
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.ui.extensions.BottomSheetHeader
import io.horizontalsystems.walletkit.uiv3.components.BoxBordered
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.Section
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsSwitcher
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.menu.MenuGroup
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object AppearancePage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        AppearanceScreen(navigation)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(navigation: HSNavigation) {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    var selectedAppIcon by remember { mutableStateOf<AppIcon?>(null) }

    var openThemeSelector by rememberSaveable { mutableStateOf(false) }
    var openLaunchPageSelector by rememberSaveable { mutableStateOf(false) }
    var openBalanceValueSelector by rememberSaveable { mutableStateOf(false) }
    var openPriceChangeIntervalSelector by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Settings_AppSettings),
        onBack = navigation::removeLastOrNull,
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Reset),
                enabled = uiState.resetEnabled,
                testTag = TestTags.APPEARANCE_RESET,
                onClick = viewModel::reset
            )
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            VSpacer(height = 12.dp)
            Section {
                DropdownCell(
                    title = R.string.Settings_Theme,
                    value = uiState.selectedTheme.title.getString(),
                    testTag = TestTags.APPEARANCE_THEME,
                    onClick = { openThemeSelector = true }
                )
            }

            VSpacer(16.dp)
            Section {
                NavigationCell(
                    title = R.string.Settings_Language,
                    value = uiState.currentLanguage,
                    testTag = TestTags.APPEARANCE_LANGUAGE,
                    onClick = {
                        navigation.slideFromRight(LanguageSettingsPage)

                        stat(
                            page = StatPage.Settings,
                            event = StatEvent.Open(StatPage.Language)
                        )
                    }
                )
                BoxBordered(top = true) {
                    NavigationCell(
                        title = R.string.Settings_BaseCurrency,
                        value = uiState.baseCurrencyCode,
                        testTag = TestTags.APPEARANCE_BASE_CURRENCY,
                        onClick = {
                            navigation.slideFromRight(BaseCurrencySettingsPage)

                            stat(
                                page = StatPage.Settings,
                                event = StatEvent.Open(StatPage.BaseCurrency)
                            )
                        }
                    )
                }
            }

            AnimatedVisibility(visible = !uiState.marketsTabHidden) {
                Column {
                    VSpacer(16.dp)
                    Section {
                        DropdownCell(
                            title = R.string.Settings_LaunchScreen,
                            subtitle = R.string.Settings_LaunchScreen_Tip,
                            value = uiState.selectedLaunchScreen.title.getString(),
                            testTag = TestTags.APPEARANCE_LAUNCH_SCREEN,
                            onClick = { openLaunchPageSelector = true }
                        )
                    }
                }
            }

            HeaderText(text = stringResource(id = R.string.Appearance_MarketsTab))
            Section {
                SwitchCell(
                    title = R.string.Appearance_HideMarketsTab,
                    subtitle = R.string.Appearance_HideMarketsTab_Tip,
                    checked = uiState.marketsTabHidden,
                    testTag = TestTags.APPEARANCE_HIDE_MARKETS,
                    onCheckedChange = viewModel::onSetMarketTabsHidden
                )
                BoxBordered(top = true) {
                    DropdownCell(
                        title = R.string.Appearance_PriceChangeInterval,
                        subtitle = R.string.Appearance_PriceChangeInterval_Tip,
                        value = uiState.priceChangeInterval.title.getString(),
                        testTag = TestTags.APPEARANCE_PRICE_CHANGE,
                        onClick = { openPriceChangeIntervalSelector = true }
                    )
                }
            }

            HeaderText(text = stringResource(id = R.string.Appearance_BalanceTab))
            Section {
                SwitchCell(
                    title = R.string.Appearance_HideBalanceTabButtons,
                    subtitle = R.string.Appearance_HideBalanceTabButtons_Tip,
                    checked = uiState.balanceTabButtonsHidden,
                    testTag = TestTags.APPEARANCE_HIDE_BUTTONS,
                    onCheckedChange = viewModel::onSetBalanceTabButtonsHidden
                )
                BoxBordered(top = true) {
                    SwitchCell(
                        title = R.string.Appearance_ChartButton,
                        subtitle = R.string.Appearance_ChartButton_Tip,
                        checked = uiState.chartButtonEnabled,
                        testTag = TestTags.APPEARANCE_CHART_BUTTON,
                        onCheckedChange = viewModel::onChartButtonToggle
                    )
                }
                BoxBordered(top = true) {
                    SwitchCell(
                        title = R.string.Appearance_AmountRounding,
                        subtitle = R.string.Appearance_AmountRounding_Tip,
                        checked = uiState.amountRoundingEnabled,
                        testTag = TestTags.APPEARANCE_AMOUNT_ROUNDING,
                        onCheckedChange = viewModel::onAmountRoundingToggle
                    )
                }
                BoxBordered(top = true) {
                    DropdownCell(
                        title = R.string.Appearance_BalanceValue,
                        subtitle = R.string.Appearance_BalanceValue_Tip,
                        value = uiState.selectedBalanceViewType.title.getString(),
                        testTag = TestTags.APPEARANCE_BALANCE_VALUE,
                        onClick = { openBalanceValueSelector = true }
                    )
                }
            }

            HeaderText(text = stringResource(id = R.string.Appearance_SendScreen))
            Section {
                SwitchCell(
                    title = R.string.Appearance_RecentlySent,
                    subtitle = R.string.Appearance_RecentlySent_Tip,
                    checked = uiState.recentlySentEnabled,
                    testTag = TestTags.APPEARANCE_RECENTLY_SENT,
                    onCheckedChange = viewModel::enableRecentlySent
                )
            }

            HeaderText(text = stringResource(id = R.string.Appearance_AppIcon))
            AppIconSection(uiState.appIconOptions) {
                scope.launch {
                    selectedAppIcon = it
                    showBottomSheet = true
                }
            }

            VSpacer(32.dp)
        }
        //Dialogs
        if (openThemeSelector) {
            MenuGroup(
                title = stringResource(R.string.Settings_Theme),
                select = uiState.themeOptions,
                onSelect = viewModel::onEnterTheme,
                onDismiss = { openThemeSelector = false },
            )
        }
        if (openLaunchPageSelector) {
            MenuGroup(
                title = stringResource(R.string.Settings_LaunchScreen),
                select = uiState.launchScreenOptions,
                onSelect = viewModel::onEnterLaunchPage,
                onDismiss = { openLaunchPageSelector = false },
            )
        }
        if (openBalanceValueSelector) {
            MenuGroup(
                title = stringResource(R.string.Appearance_BalanceValue),
                select = uiState.balanceViewTypeOptions,
                onSelect = viewModel::onEnterBalanceViewType,
                onDismiss = { openBalanceValueSelector = false },
            )
        }
        if (openPriceChangeIntervalSelector) {
            MenuGroup(
                title = stringResource(R.string.Appearance_PriceChangeInterval),
                select = uiState.priceChangeIntervalOptions,
                onSelect = viewModel::onSetPriceChangeInterval,
                onDismiss = { openPriceChangeIntervalSelector = false },
            )
        }

        if (showBottomSheet) {
            BottomSheetContent(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                AppCloseWarningBottomSheet(
                    onCloseClick = {
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    },
                    onChangeClick = {
                        selectedAppIcon?.let { viewModel.onEnterAppIcon(it) }
                        scope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}

// A row whose value sits in a dropdown button. The whole row opens the
// options as well, since flows and users tap it by its title.
@Composable
private fun DropdownCell(
    @StringRes title: Int,
    @StringRes subtitle: Int? = null,
    value: String,
    testTag: String,
    onClick: () -> Unit,
) {
    CellPrimary(
        modifier = Modifier.heightIn(min = 64.dp),
        middle = {
            CellMiddleInfo(
                title = stringResource(title).hs,
                subtitle = subtitle?.let { stringResource(it).hs },
            )
        },
        right = {
            HSDropdownButton(
                modifier = Modifier.testTag(testTag),
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                title = value,
                onClick = onClick,
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun SwitchCell(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    CellPrimary(
        modifier = Modifier.heightIn(min = 64.dp),
        middle = {
            CellMiddleInfo(
                title = stringResource(title).hs,
                subtitle = stringResource(subtitle).hs,
            )
        },
        right = {
            CellRightControlsSwitcher(
                modifier = Modifier.testTag(testTag),
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun NavigationCell(
    @StringRes title: Int,
    value: String,
    testTag: String,
    onClick: () -> Unit,
) {
    CellPrimary(
        modifier = Modifier
            .heightIn(min = 64.dp)
            .testTag(testTag),
        middle = {
            CellMiddleInfo(title = stringResource(title).hs)
        },
        right = {
            CellRightNavigation(subtitle = value.hs)
        },
        onClick = onClick,
    )
}

@Composable
private fun AppCloseWarningBottomSheet(
    onCloseClick: () -> Unit,
    onChangeClick: () -> Unit
) {
    BottomSheetHeader(
        iconPainter = painterResource(id = R.drawable.ic_attention_24),
        title = stringResource(id = R.string.Alert_TitleWarning),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
        onCloseClick = onCloseClick
    ) {
        TextImportantWarning(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            text = stringResource(R.string.Appearance_Warning_CloseApplication)
        )

        HSButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp),
            title = stringResource(id = R.string.Button_Change),
            onClick = onChangeClick
        )

        HSButton(
            variant = ButtonVariant.Secondary,
            style = ButtonStyle.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            title = stringResource(id = R.string.Button_Cancel),
            onClick = onCloseClick
        )
        VSpacer(20.dp)
    }
}

// Spreads the tiles from edge to edge like SpaceBetween, and keeps a gap
// between them when the row is too narrow for the tiles at full size
private val AppIconsArrangement = object : Arrangement.Horizontal {
    override val spacing = 8.dp

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray
    ) = with(Arrangement.SpaceBetween) {
        arrange(totalSize, sizes, layoutDirection, outPositions)
    }
}

@Composable
private fun AppIconSection(appIconOptions: Select<AppIcon>, onAppIconSelect: (AppIcon) -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(16.dp)
            .selectableGroup(),
        horizontalArrangement = AppIconsArrangement
    ) {
        appIconOptions.options.forEach { appIcon ->
            IconBox(
                // Tiles keep their size while the row has room for them and
                // shrink evenly, instead of squeezing the last one, when a
                // narrow window does not
                modifier = Modifier.weight(1f, fill = false),
                icon = appIcon.icon,
                name = appIcon.title.getString(),
                selected = appIcon == appIconOptions.selected,
                testTag = TestTags.appIcon(appIcon.titleText.lowercase()),
                onAppIconSelect = { onAppIconSelect(appIcon) }
            )
        }
    }
}

@Composable
private fun IconBox(
    modifier: Modifier,
    icon: Int,
    name: String,
    selected: Boolean,
    testTag: String,
    onAppIconSelect: () -> Unit
) {
    Column(
        modifier = modifier
            .testTag(testTag)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onAppIconSelect
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            modifier = Modifier
                .widthIn(max = 60.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            painter = painterResource(icon),
            contentDescription = null,
        )
        if (selected) {
            subhead1_orange(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else {
            subhead1_leah(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
