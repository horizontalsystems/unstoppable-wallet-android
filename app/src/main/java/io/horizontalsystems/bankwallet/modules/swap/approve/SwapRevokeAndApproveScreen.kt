package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.requestKey
import io.horizontalsystems.bankwallet.modules.swap.approve.SwapApproveModule.resultKey
import io.horizontalsystems.bankwallet.modules.swap.approve.confirmation.SwapApproveConfirmationModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.getNavigationResult
import io.horizontalsystems.core.setNavigationResult

@Composable
fun SwapRevokeAndApproveScreen(
    navController: NavController,
    approveData: SwapAllowanceService.ApproveData
) {
    val viewModel =
        viewModel<SwapRevokeAndApproveViewModel>(factory = SwapRevokeAndApproveViewModel.Factory(approveData))

    val uiState = viewModel.uiState

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.ResString(R.string.Approve_Title),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = navController::popBackStack
                        )
                    )
                )
            }
        ) {
            Column(Modifier.padding(it)) {
                Spacer(modifier = Modifier.height(12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.Approve_RevokeAndApproveInfo, viewModel.allowanceValue.getFormattedFull())
                )

                AnimatedVisibility(uiState.showAmountInput) {
                    Column {
                        Spacer(modifier = Modifier.height(32.dp))
                        var validAmount by rememberSaveable { mutableStateOf(viewModel.requiredAmountStr) }
                        FormsInput(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            initial = viewModel.requiredAmountStr,
                            hint = "",
                            pasteEnabled = false,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textPreprocessor = object : TextPreprocessor {
                                override fun process(text: String): String {
                                    if (viewModel.validateAmount(text)) {
                                        validAmount = text
                                    } else {
                                        // todo: shake animation
                                    }
                                    return validAmount
                                }
                            },
                            onValueChange = {
                                viewModel.onEnterAmount(it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    ButtonPrimaryDefault(
                        modifier = Modifier.weight(1f),
                        title = stringResource(if (uiState.revokeInProgress) R.string.Approve_ButtonRevoking else R.string.Approve_ButtonRevoke),
                        onClick = {
                            navigateToSwapRevokeConfirm(navController, viewModel)
                        },
                        enabled = uiState.revokeEnabled
                    )
                    Spacer(Modifier.width(4.dp))
                    ButtonPrimaryYellow(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.Approve_ButtonApprove),
                        onClick = {
                            navigateToSwapApproveConfirm(navController, viewModel)
                        },
                        enabled = uiState.approveEnabled
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.steps.forEachIndexed { i, step ->
                        if (i != 0) {
                            Divider(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .background(ComposeAppTheme.colors.steel20)
                                    .height(2.dp)
                            )
                        }
                        BadgeStepCircle(text = step, active = step == uiState.currentStep)
                    }
                }
            }
        }
    }
}

private fun navigateToSwapRevokeConfirm(navController: NavController, viewModel: SwapRevokeAndApproveViewModel) {
    navController.getNavigationResult(requestKey) { result ->
        if (result.getBoolean(resultKey)) {
            viewModel.onRevokeTransactionSend()
        }
    }

    navController.slideFromRight(
        R.id.swapApproveConfirmationFragment,
        SwapApproveConfirmationModule.prepareParams(
            viewModel.getRevokeSendEvmData(),
            viewModel.blockchainType
        )
    )
}

private fun navigateToSwapApproveConfirm(navController: NavController, viewModel: SwapRevokeAndApproveViewModel) {
    navController.getNavigationResult(requestKey) { result ->
        if (result.getBoolean(resultKey)) {
            navController.setNavigationResult(requestKey, bundleOf(resultKey to true))
            navController.popBackStack(R.id.swapFragment, false)
        }
    }

    navController.slideFromRight(
        R.id.swapApproveConfirmationFragment,
        SwapApproveConfirmationModule.prepareParams(
            viewModel.getApproveSendEvmData(),
            viewModel.blockchainType
        )
    )
}
