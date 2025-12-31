package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.stringResId
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationScreen
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun SendBitcoinConfirmationScreen(
    navController: NavController,
    sendViewModel: SendBitcoinViewModel,
    sendEntryPointDestId: Int
) {
    var confirmationData by remember { mutableStateOf(sendViewModel.getConfirmationData()) }
    var refresh by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        if (refresh) {
            confirmationData = sendViewModel.getConfirmationData()
        }

        onPauseOrDispose {
            refresh = true
        }
    }

    SendConfirmationScreen(
        navController = navController,
        coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        feeCoinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals,
        rate = sendViewModel.coinRate,
        feeCoinRate = sendViewModel.coinRate,
        sendResult = sendViewModel.sendResult,
        token = confirmationData.token,
        feeCoin = confirmationData.feeCoin,
        amount = confirmationData.amount,
        address = confirmationData.address,
        contact = confirmationData.contact,
        fee = confirmationData.fee,
        memo = confirmationData.memo,
        onClickSend = sendViewModel::onClickSend,
        sendEntryPointDestId = sendEntryPointDestId,
    ) {
        sendViewModel.uiState.utxoData?.let { utxo ->
            QuoteInfoRow(
                title = stringResource(R.string.Send_Utxos),
                value = utxo.value.hs,
            )
        }
        confirmationData.lockTimeInterval?.let { interval ->
            QuoteInfoRow(
                title = stringResource(R.string.Send_DialogLockTime),
                value = stringResource(interval.stringResId()).hs,
            )
        }
        confirmationData.rbfEnabled?.let { enabled ->
            val value =
                stringResource(if (enabled) R.string.Send_RbfEnabled else R.string.Send_RbfDisabled)
            QuoteInfoRow(
                title = stringResource(R.string.Send_Rbf),
                value = value.hs,
            )
        }
    }
}