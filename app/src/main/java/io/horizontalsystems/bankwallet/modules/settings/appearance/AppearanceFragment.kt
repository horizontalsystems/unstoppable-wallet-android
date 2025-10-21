package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.coroutines.launch

class AppearanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AppearanceScreen(navController)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(navController: NavController) {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    var selectedAppIcon by remember { mutableStateOf<AppIcon?>(null) }

    var openThemeSelector by rememberSaveable { mutableStateOf(false) }
    var openLaunchPageSelector by rememberSaveable { mutableStateOf(false) }
    var openBalanceValueSelector by rememberSaveable { mutableStateOf(false) }
    var openPriceChangeIntervalSelector by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Settings_AppSettings),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
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

            VSpacer(32.dp)

            CellUniversalLawrenceSection(
                buildList {
                    add {
                        MenuItem(
                            R.string.Settings_Language,
                            value = uiState.currentLanguage,
                            onClick = {
                                navController.slideFromRight(R.id.languageSettingsFragment)

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.Language)
                                )
                            }
                        )
                    }
                    add {
                        MenuItem(
                            R.string.Settings_BaseCurrency,
                            value = uiState.baseCurrencyCode,
                            onClick = {
                                navController.slideFromRight(R.id.baseCurrencySettingsFragment)

                                stat(
                                    page = StatPage.Settings,
                                    event = StatEvent.Open(StatPage.BaseCurrency)
                                )
                            }
                        )
                    }
                }
            )

            VSpacer(24.dp)

            HeaderText(text = stringResource(id = R.string.Appearance_MarketsTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_HideMarketsTab,
                            subtitle = R.string.Appearance_HideMarketsTab_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.marketsTabHidden,
                                onCheckedChange = {
                                    viewModel.onSetMarketTabsHidden(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_PriceChangeInterval,
                            subtitle = R.string.Appearance_PriceChangeInterval_Tip,
                            onClick = { openPriceChangeIntervalSelector = true }
                        ) {
                            subhead1_leah(
                                text = uiState.priceChangeInterval.title.getString(),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                            )
                        }
                    }
                )
            )

            AnimatedVisibility(visible = !uiState.marketsTabHidden) {
                Column {
                    VSpacer(32.dp)
                    CellUniversalLawrenceSection(
                        listOf {
                            SettingUniversalCell(
                                title = R.string.Settings_LaunchScreen,
                                subtitle = R.string.Settings_LaunchScreen_Tip,
                                onClick = { openLaunchPageSelector = true }
                            ) {
                                subhead1_leah(
                                    text = uiState.selectedLaunchScreen.title.getString(),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Image(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                    contentDescription = null,
                                )
                            }
                        }
                    )
                }
            }

            VSpacer(24.dp)
            HeaderText(text = stringResource(id = R.string.Appearance_BalanceTab))
            CellUniversalLawrenceSection(
                listOf(
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_HideBalanceTabButtons,
                            subtitle = R.string.Appearance_HideBalanceTabButtons_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.balanceTabButtonsHidden,
                                onCheckedChange = {
                                    viewModel.onSetBalanceTabButtonsHidden(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_AmountRounding,
                            subtitle = R.string.Appearance_AmountRounding_Tip,
                        ) {
                            HsSwitch(
                                checked = uiState.amountRoundingEnabled,
                                onCheckedChange = {
                                    viewModel.onAmountRoundingToggle(it)
                                }
                            )
                        }
                    },
                    {
                        SettingUniversalCell(
                            title = R.string.Appearance_BalanceValue,
                            subtitle = R.string.Appearance_BalanceValue_Tip,
                            onClick = { openBalanceValueSelector = true }
                        ) {
                            subhead1_leah(
                                text = uiState.selectedBalanceViewType.title.getString(),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Image(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_down_arrow_20),
                                contentDescription = null,
                            )
                        }
                    }
                )
            )

            VSpacer(24.dp)
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
            AlertGroup(
                stringResource(R.string.Settings_Theme),
                uiState.themeOptions,
                { selected ->
                    viewModel.onEnterTheme(selected)
                    openThemeSelector = false
                },
                { openThemeSelector = false }
            )
        }
        if (openLaunchPageSelector) {
            AlertGroup(
                stringResource(R.string.Settings_LaunchScreen),
                uiState.launchScreenOptions,
                { selected ->
                    viewModel.onEnterLaunchPage(selected)
                    openLaunchPageSelector = false
                },
                { openLaunchPageSelector = false }
            )
        }
        if (openBalanceValueSelector) {
            AlertGroup(
                stringResource(R.string.Appearance_BalanceValue),
                uiState.balanceViewTypeOptions,
                { selected ->
                    viewModel.onEnterBalanceViewType(selected)
                    openBalanceValueSelector = false
                },
                { openBalanceValueSelector = false }
            )
        }
        if (openPriceChangeIntervalSelector) {
            AlertGroup(
                stringResource(R.string.Appearance_PriceChangeInterval),
                uiState.priceChangeIntervalOptions,
                { selected ->
                    viewModel.onSetPriceChangeInterval(selected)
                    openPriceChangeIntervalSelector = false
                },
                { openPriceChangeIntervalSelector = false }
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState,
                containerColor = ComposeAppTheme.colors.transparent
            ) {
                AppCloseWarningBottomSheet(
                    onCloseClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    },
                    onChangeClick = {
                        selectedAppIcon?.let { viewModel.onEnterAppIcon(it) }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingUniversalCell(
    title: Int,
    subtitle: Int? = null,
    onClick: (() -> Unit)? = null,
    value: @Composable() (RowScope.() -> Unit),
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            body_leah(text = stringResource(title))
            subtitle?.let {
                VSpacer(height = 1.dp)
                subhead2_grey(text = stringResource(it))
            }
        }
        Row(
            content = value
        )
    }
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

        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp),
            title = stringResource(id = R.string.Button_Change),
            onClick = onChangeClick
        )

        ButtonPrimaryTransparent(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            title = stringResource(id = R.string.Button_Cancel),
            onClick = onCloseClick
        )
        VSpacer(20.dp)
    }
}

@Composable
private fun AppIconSection(appIconOptions: Select<AppIcon>, onAppIconSelect: (AppIcon) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val rows = appIconOptions.options.chunked(3)
        rows.forEach { row ->
            AppIconsRow(row, appIconOptions.selected, onAppIconSelect)
        }

    }
}

@Composable
private fun AppIconsRow(
    chunk: List<AppIcon?>,
    selected: AppIcon,
    onAppIconSelect: (AppIcon) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0 until 3) {
            val appIcon = chunk.getOrNull(i)
            if (appIcon != null) {
                IconBox(
                    appIcon.icon,
                    appIcon.title.getString(),
                    appIcon == selected
                ) { onAppIconSelect(appIcon) }
            } else {
                // Invisible element to preserve space
                Spacer(modifier = Modifier.size(60.dp))
            }
        }
    }
}

@Composable
private fun IconBox(
    icon: Int,
    name: String,
    selected: Boolean,
    onAppIconSelect: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onAppIconSelect() }
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(60.dp),
            painter = painterResource(icon),
            contentDescription = null,
        )
        Box(
            Modifier
                .height(6.dp)
                .background(ComposeAppTheme.colors.red50)
        )
        if (selected) {
            subhead1_jacob(name)
        } else {
            subhead1_leah(name)
        }
    }

}

@Composable
fun MenuItemWithDialog(
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
            contentDescription = null,
        )
    }
}

@Composable
private fun MenuItem(
    @StringRes title: Int,
    value: String? = null,
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
        value?.let {
            subhead1_grey(
                text = value,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}