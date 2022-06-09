package io.horizontalsystems.bankwallet.modules.settings.appearance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.findNavController

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

@Composable
fun AppearanceScreen(navController: NavController) {
    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
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

                AppearanceScreenContent()
            }
        }
    }
}

@Composable
private fun AppearanceScreenContent() {
    val viewModel = viewModel<AppearanceViewModel>(factory = AppearanceModule.Factory())
    val uiState = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HeaderText(text = stringResource(id = R.string.Appearance_Theme))
        CellSingleLineLawrenceSection(uiState.themeOptions.options) { option: ThemeType ->
            RowSelect(
                painter = painterResource(id = option.iconRes),
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
                painter = painterResource(id = option.iconRes),
                text = option.title.getString(),
                selected = option == uiState.launchScreenOptions.selected
            ) {
                viewModel.onEnterLaunchPage(option)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        HeaderText(text = stringResource(id = R.string.Appearance_BalanceConversion))
        CellSingleLineLawrenceSection(uiState.baseCoinOptions.options) { option ->
            RowSelect(
                painter = painterResource(id = R.drawable.coin_placeholder),
                text = option.coin.code,
                selected = option == uiState.baseCoinOptions.selected
            ) {
                viewModel.onEnterBaseCoin(option)
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
private fun RowSelect(
    painter: Painter,
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
        Image(
            painter = painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(ComposeAppTheme.colors.grey)
        )
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
