package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.SendScreenCommon
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.BtcTransactionInputSortInfoScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.SendBtcAdvancedSettingsScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.utxoexpert.UtxoExpertModeScreen
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah


const val SendBtcPage = "send_btc"
const val SendBtcAdvancedSettingsPage = "send_btc_advanced_settings"
const val TransactionInputsSortInfoPage = "transaction_input_sort_info_settings"
const val UtxoExpertModePage = "utxo_expert_mode_page"

@Composable
fun SendBitcoinNavHost(
    title: String,
    fragmentNavController: NavController,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    prefilledData: PrefilledData?,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = SendBtcPage,
    ) {
        composable(SendBtcPage) {
            SendBitcoinScreen(
                title,
                fragmentNavController,
                navController,
                viewModel,
                amountInputModeViewModel,
                sendEntryPointDestId,
                prefilledData,
            )
        }
        composablePage(SendBtcAdvancedSettingsPage) {
            SendBtcAdvancedSettingsScreen(
                fragmentNavController = fragmentNavController,
                navController = navController,
                sendBitcoinViewModel = viewModel,
                amountInputType = amountInputModeViewModel.inputType,
            )
        }
        composablePopup(TransactionInputsSortInfoPage) { BtcTransactionInputSortInfoScreen { navController.popBackStack() } }
        composablePage(UtxoExpertModePage) {
            UtxoExpertModeScreen(
                adapter = viewModel.adapter,
                token = viewModel.wallet.token,
                address = viewModel.uiState.address,
                memo = viewModel.uiState.memo,
                value = viewModel.uiState.amount,
                feeRate = viewModel.uiState.feeRate,
                customUnspentOutputs = viewModel.customUnspentOutputs,
                updateUnspentOutputs = {
                    viewModel.updateCustomUnspentOutputs(it)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun SendBitcoinScreen(
    title: String,
    fragmentNavController: NavController,
    composeNavController: NavController,
    viewModel: SendBitcoinViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int,
    prefilledData: PrefilledData?,
) {
    val wallet = viewModel.wallet
    val uiState = viewModel.uiState

    SendScreenCommon(
        navController = fragmentNavController,
        wallet = wallet,
        prefilledData = prefilledData,
        title = title,
        coinDecimals = viewModel.coinMaxAllowedDecimals,
        onEnterAmount = viewModel::onEnterAmount,
        onEnterAmountPercentage = viewModel::onEnterAmountPercentage,
        onEnterFiatAmount = {  },
        onEnterAddress = viewModel::onEnterAddress,
        onProceed = {
            fragmentNavController.slideFromRight(
                R.id.sendConfirmation,
                SendConfirmationFragment.Input(SendConfirmationFragment.Type.Bitcoin, sendEntryPointDestId)
            )
        },
        uiState = uiState.sendUiState()
    )
}

@Composable
fun UtxoCell(
    utxoData: SendBitcoinModule.UtxoData,
    onClick: () -> Unit
) {
    RowUniversal(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick
    ) {
        subhead2_grey(
            text = stringResource(R.string.Send_Utxos),
            modifier = Modifier.weight(1f)
        )
        subhead2_leah(text = utxoData.value)
        HSpacer(8.dp)
        when (utxoData.type) {
            SendBitcoinModule.UtxoType.Auto -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }

            SendBitcoinModule.UtxoType.Manual -> {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.jacob
                )
            }

            null -> {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}
