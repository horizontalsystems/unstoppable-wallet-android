package io.horizontalsystems.bankwallet.modules.settings.security.autolock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.findNavController

class AutoLockIntervalsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        AutoLockIntervalsScreen(
            close = { findNavController().popBackStack() },
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
    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
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
