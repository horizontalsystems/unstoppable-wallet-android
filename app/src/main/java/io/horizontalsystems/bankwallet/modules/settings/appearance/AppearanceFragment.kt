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
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
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
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.AlertGroup
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.HsSwitch
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import kotlinx.coroutines.launch

class AppearanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AppearanceScreen(navController)
    }

}

@Composable
fun AppearanceScreen(navController: NavController) {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    val scope = rememberCoroutineScope()
    var selectedAppIcon by remember { mutableStateOf<AppIcon?>(null) }
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden
    )

    var openThemeSelector by rememberSaveable { mutableStateOf(false) }
    var openLaunchPageSelector by rememberSaveable { mutableStateOf(false) }
    var openBalanceValueSelector by rememberSaveable { mutableStateOf(false) }
    var openPriceChangeIntervalSelector by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetBackgroundColor = ComposeAppTheme.colors.transparent,
        sheetContent = {
            AppCloseWarningBottomSheet(
                onCloseClick = { scope.launch { sheetState.hide() } },
                onChangeClick = {
                    selectedAppIcon?.let { viewModel.onEnterAppIcon(it) }
                    scope.launch { sheetState.hide() }
                }
            )
        }
    ) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.Settings_Appearance),
                    navigationIcon = {
                        HsBackButton(onClick = { navController.popBackStack() })
                    },
                    menuItems = listOf(),
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(paddingValues),
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
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                body_leah(
                                    text = stringResource(id = R.string.Appearance_HideMarketsTab),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 16.dp)
                                )
                                HsSwitch(
                                    checked = uiState.marketsTabHidden,
                                    onCheckedChange = {
                                        viewModel.onSetMarketTabsHidden(it)
                                    }
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
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                body_leah(
                                    text = stringResource(id = R.string.Appearance_HideBalanceTabButtons),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 16.dp)
                                )
                                HsSwitch(
                                    checked = uiState.balanceTabButtonsHidden,
                                    onCheckedChange = {
                                        viewModel.onSetBalanceTabButtonsHidden(it)
                                    }
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

                VSpacer(24.dp)
                HeaderText(text = stringResource(id = R.string.Appearance_AppIcon))
                AppIconSection(uiState.appIconOptions) {
                    scope.launch {
                        selectedAppIcon = it
                        sheetState.show()
                    }
                }

                VSpacer(32.dp)
            }
        }
        //Dialogs
        if (openThemeSelector) {
            AlertGroup(
                R.string.Settings_Theme,
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
                R.string.Settings_LaunchScreen,
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
                R.string.Appearance_BalanceValue,
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
                R.string.Appearance_PriceChangeInterval,
                uiState.priceChangeIntervalOptions,
                { selected ->
                    viewModel.onSetPriceChangeInterval(selected)
                    openPriceChangeIntervalSelector = false
                },
                { openPriceChangeIntervalSelector = false }
            )
        }
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
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        val rows = appIconOptions.options.chunked(3)
        AppIconsRow(rows[0], appIconOptions.selected, onAppIconSelect)
        AppIconsRow(rows[1], appIconOptions.selected, onAppIconSelect)
        AppIconsRow(rows[2], appIconOptions.selected, onAppIconSelect)
        AppIconsRow(rows[3], appIconOptions.selected, onAppIconSelect)
        AppIconsRow(rows[4], appIconOptions.selected, onAppIconSelect)
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