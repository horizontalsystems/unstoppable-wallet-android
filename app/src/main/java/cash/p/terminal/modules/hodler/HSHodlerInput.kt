package cash.p.terminal.modules.hodler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.core.stringResId
import cash.p.terminal.ui.compose.components.ButtonSecondaryWithIcon
import io.horizontalsystems.core.RowUniversal
import cash.p.terminal.ui.compose.components.SelectorDialogCompose
import cash.p.terminal.ui.compose.components.SelectorItem
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.hodler.LockTimeInterval

@Composable
fun HSHodlerInput(
    lockTimeIntervals: List<LockTimeInterval?>,
    lockTimeInterval: LockTimeInterval?,
    onSelect: (LockTimeInterval?) -> Unit
) {
    var showSelectorDialog by remember { mutableStateOf(false) }
    if (showSelectorDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Send_DialogSpeed),
            items = lockTimeIntervals.map {
                SelectorItem(stringResource(it.stringResId()), it == lockTimeInterval, it)
            },
            onDismissRequest = {
                showSelectorDialog = false
            },
            onSelectItem = {
                onSelect.invoke(it)
            }
        )
    }

    RowUniversal(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { showSelectorDialog = true }
        ),
    ) {
        body_leah(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.Send_DialogLockTime),
        )
        Spacer(modifier = Modifier.weight(1f))
        ButtonSecondaryWithIcon(
            modifier = Modifier.height(28.dp),
            onClick = { showSelectorDialog = true },
            title = stringResource(lockTimeInterval.stringResId()),
            iconRight = painterResource(R.drawable.ic_down_arrow_20),
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun HSHodler(
    lockTimeInterval: LockTimeInterval,
) {
    RowUniversal {
        Icon(
            modifier = Modifier.padding(horizontal = 16.dp),
            painter = painterResource(id = R.drawable.ic_lock_20),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = "lock icon",
        )
        subhead2_grey(
            modifier = Modifier.padding(end = 16.dp),
            text = stringResource(R.string.Send_DialogLockTime),
        )
        Spacer(modifier = Modifier.weight(1f))
        subhead1_leah(text = stringResource(lockTimeInterval.stringResId()))
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_HSHodlerInput() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        HSHodlerInput(
            listOf(LockTimeInterval.hour, LockTimeInterval.halfYear),
            LockTimeInterval.halfYear,
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_HSHodler() {
    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        HSHodler(LockTimeInterval.halfYear)
    }
}