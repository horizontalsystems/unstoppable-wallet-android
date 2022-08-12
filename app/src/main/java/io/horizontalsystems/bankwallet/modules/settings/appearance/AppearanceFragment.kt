package io.horizontalsystems.bankwallet.modules.settings.appearance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import kotlinx.coroutines.launch

class AppearanceFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                AppearanceScreen(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppearanceScreen(navController: NavController) {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    val scope = rememberCoroutineScope()
    var selectedAppIcon by remember { mutableStateOf<AppIcon?>(null) }
    val sheetState = rememberModalBottomSheetState(
        ModalBottomSheetValue.Hidden
    )

    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
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
                Column {
                    AppBar(
                        TranslatableString.ResString(R.string.Settings_Appearance),
                        navigationIcon = {
                            HsIconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_back),
                                    contentDescription = "back button",
                                    tint = ComposeAppTheme.colors.jacob
                                )
                            }
                        },
                        menuItems = listOf(),
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        HeaderText(text = stringResource(id = R.string.Appearance_Theme))
                        CellSingleLineLawrenceSection(uiState.themeOptions.options) { option: ThemeType ->
                            RowSelect(
                                imageContent = {
                                    Image(
                                        painter = painterResource(id = option.iconRes),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
                                    )
                                },
                                text = option.title.getString(),
                                selected = option == uiState.themeOptions.selected
                            ) {
                                viewModel.onEnterTheme(option)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        HeaderText(text = stringResource(id = R.string.Appearance_LaunchScreen))
                        CellSingleLineLawrenceSection(uiState.launchScreenOptions.options) { option ->
                            RowSelect(
                                imageContent = {
                                    Image(
                                        painter = painterResource(id = option.iconRes),
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
                                    )
                                },
                                text = option.title.getString(),
                                selected = option == uiState.launchScreenOptions.selected
                            ) {
                                viewModel.onEnterLaunchPage(option)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        HeaderText(text = stringResource(id = R.string.Appearance_AppIcon))
                        AppIconSection(uiState.appIconOptions) {
                            scope.launch {
                                selectedAppIcon = it
                                sheetState.show()
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        HeaderText(text = stringResource(id = R.string.Appearance_BalanceConversion))
                        CellSingleLineLawrenceSection(uiState.baseTokenOptions.options) { option ->
                            RowSelect(
                                imageContent = {
                                    CoinImage(
                                        iconUrl = option.coin.iconUrl,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                text = option.coin.code,
                                selected = option == uiState.baseTokenOptions.selected
                            ) {
                                viewModel.onEnterBaseToken(option)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        HeaderText(text = stringResource(id = R.string.Appearance_BalanceValue))
                        CellMultilineLawrenceSection(uiState.balanceViewTypeOptions.options) { option ->
                            RowMultilineSelect(
                                title = stringResource(id = option.titleResId),
                                subtitle = stringResource(id = option.subtitleResId),
                                selected = option == uiState.balanceViewTypeOptions.selected
                            ) {
                                viewModel.onEnterBalanceViewType(option)
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
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
        Spacer(modifier = Modifier.height(20.dp))
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
    }
}

@Composable
private fun AppIconsRow(
    chunk: List<AppIcon>,
    selected: AppIcon,
    onAppIconSelect: (AppIcon) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconBox(
            chunk[0].icon,
            chunk[0].title.getString(),
            chunk[0] == selected,
        ) { onAppIconSelect(chunk[0]) }
        IconBox(
            chunk[1].icon,
            chunk[1].title.getString(),
            chunk[1] == selected,
        ) { onAppIconSelect(chunk[1]) }
        IconBox(
            chunk[2].icon,
            chunk[2].title.getString(),
            chunk[2] == selected
        ) { onAppIconSelect(chunk[2]) }
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
        Box(Modifier.height(6.dp).background(ComposeAppTheme.colors.red50))
        if (selected) {
            subhead1_jacob(name)
        } else {
            subhead1_leah(name)
        }
    }

}

@Composable
private fun RowMultilineSelect(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MultitextM1(
            title = { B2(text = title) },
            subtitle = { D1(text = subtitle) }
        )
        Spacer(modifier = Modifier.weight(1f))
        if (selected) {
            Image(
                painter = painterResource(id = R.drawable.ic_checkmark_20),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob)
            )
        }
    }
}

@Composable
fun RowSelect(
    imageContent: @Composable RowScope.() -> Unit,
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        imageContent.invoke(this)
        body_leah(
            text = text,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )
        if (selected) {
            Image(
                painter = painterResource(id = R.drawable.ic_checkmark_20),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob)
            )
        }
    }
}
