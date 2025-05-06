package io.horizontalsystems.bankwallet.modules.receive.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction

@Composable
fun ReceiveTokenActivationRequired(onClickActivate: () -> Unit) {
    ScreenMessageWithAction(
        text = stringResource(R.string.Balance_Receive_ActivationRequired),
        icon = R.drawable.ic_sync_error,
    ) {
        ButtonPrimaryYellow(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .fillMaxWidth(),
            title = stringResource(R.string.Button_Activate),
            onClick = onClickActivate
        )
    }
}