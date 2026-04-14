package com.quantum.wallet.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.stringResId
import com.quantum.wallet.bankwallet.modules.multiswap.QuoteInfoRow
import com.quantum.wallet.bankwallet.modules.send.SendConfirmationScreen
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

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
                value = utxo.value.hs(ComposeAppTheme.colors.leah),
            )
        }
        confirmationData.lockTimeInterval?.let { interval ->
            QuoteInfoRow(
                title = stringResource(R.string.Send_DialogLockTime),
                value = stringResource(interval.stringResId()).hs(ComposeAppTheme.colors.leah),
            )
        }
        confirmationData.rbfEnabled?.let { enabled ->
            val value =
                stringResource(if (enabled) R.string.Send_RbfEnabled else R.string.Send_RbfDisabled)
            QuoteInfoRow(
                title = stringResource(R.string.Send_Rbf),
                value = value.hs(ComposeAppTheme.colors.leah),
            )
        }
    }
}