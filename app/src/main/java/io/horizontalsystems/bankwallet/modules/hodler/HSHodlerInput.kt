package io.horizontalsystems.bankwallet.modules.hodler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.hodler.LockTimeInterval

@Composable
fun HSHodlerInput(
    lockTimeIntervals: List<LockTimeInterval?> = listOf(),
    lockTimeInterval: LockTimeInterval?,
    onSelect: ((LockTimeInterval?) -> Unit)? = null
) {
    var showSelectorDialog by remember { mutableStateOf(false) }
    if (showSelectorDialog) {
        SelectorDialogCompose(
            title = stringResource(R.string.Send_DialogSpeed),
            items = lockTimeIntervals.map {
                TabItem(stringResource(it.stringResId()), it == lockTimeInterval, it)
            },
            onDismissRequest = {
                showSelectorDialog = false
            },
            onSelectItem = {
                onSelect?.invoke(it)
            }
        )
    }

    val selectable = lockTimeIntervals.isNotEmpty()
    val modifierClickable = if (selectable) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                showSelectorDialog = true
            }
        )
    } else {
        Modifier
    }

    RowUniversal(
        modifier = Modifier.then(modifierClickable),
    ) {
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
        if (selectable) {
            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(R.drawable.ic_down_arrow_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}