package cash.p.terminal.modules.settings.security.autolock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

class AutoLockIntervalsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AutoLockIntervalsScreen(
            close = { navController.popBackStack() },
        )
    }

}

@Composable
private fun AutoLockIntervalsScreen(
    close: () -> Unit,
    viewModel: AutoLockIntervalsViewModel = viewModel(
        factory = AutoLockModule.Factory()
    )
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_AutoLock),
                navigationIcon = {
                    HsBackButton(onClick = close)
                },
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                VSpacer(12.dp)
                CellUniversalLawrenceSection(viewModel.intervals) { item ->
                    IntervalCell(item.interval, item.selected) { interval ->
                        viewModel.onSelectAutoLockInterval(interval)
                        close.invoke()
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalCell(
    item: AutoLockInterval,
    checked: Boolean,
    onClick: (AutoLockInterval) -> Unit
) {
    RowUniversal(
        onClick = { onClick.invoke(item) }
    ) {
        HSpacer(16.dp)
        body_leah(
            modifier = Modifier.weight(1f),
            text = stringResource(item.title)
        )
        Box(
            modifier = Modifier
                .width(52.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    painter = painterResource(R.drawable.ic_checkmark_20),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }
    }
}
