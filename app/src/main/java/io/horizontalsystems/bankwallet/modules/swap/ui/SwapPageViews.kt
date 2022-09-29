package io.horizontalsystems.bankwallet.modules.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun SwapError(swapError: String?) {
    swapError?.let { error ->
        Spacer(Modifier.height(12.dp))
        AdditionalDataCell2 {
            subhead2_lucian(text = error)
        }
    }
}

@Composable
fun SwitchCoinsSection(
    showProgressbar: Boolean,
    onSwitchButtonClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        if (showProgressbar) {
            Box(Modifier.padding(top = 8.dp)) {
                HSCircularProgressIndicator()
            }
        }
        HsIconButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = onSwitchButtonClick,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_switch),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }
    }
}

@Composable
fun SwapAllowance(viewModel: SwapAllowanceViewModel) {
    val uiState = viewModel.uiState
    val isError = uiState.isError
    val revokeRequired = uiState.revokeRequired
    val allowanceAmount = uiState.allowance
    val visible = uiState.isVisible

    if (visible) {
        Spacer(Modifier.height(12.dp))
        if (revokeRequired) {
            TextImportantWarning(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.Approve_RevokeAndApproveInfo, allowanceAmount ?: "")
            )
        } else {
            AdditionalDataCell2 {
                subhead2_grey(text = stringResource(R.string.Swap_Allowance))
                Spacer(Modifier.weight(1f))
                allowanceAmount?.let { amount ->
                    if (isError) {
                        subhead2_lucian(text = amount)
                    } else {
                        subhead2_grey(text = amount)
                    }
                }
            }
        }
    }
}

@Composable
fun SwapAllowanceSteps(approveStep: SwapMainModule.ApproveStep?) {
    val step1Active: Boolean
    val step2Active: Boolean
    when (approveStep) {
        SwapMainModule.ApproveStep.ApproveRequired, SwapMainModule.ApproveStep.Approving -> {
            step1Active = true
            step2Active = false
        }
        SwapMainModule.ApproveStep.Approved -> {
            step1Active = false
            step2Active = true
        }
        SwapMainModule.ApproveStep.NA, null -> {
            return
        }
    }

    Spacer(Modifier.height(24.dp))
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgeStepCircle(text = "1", active = step1Active)
        Divider(
            Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .background(ComposeAppTheme.colors.steel20)
                .height(2.dp)
        )
        BadgeStepCircle(text = "2", active = step2Active)
    }
}

@Composable
fun ActionButtons(
    buttons: SwapButtons?,
    onTapRevoke: () -> Unit,
    onTapApprove: () -> Unit,
    onTapProceed: () -> Unit,
) {
    buttons?.let { actionButtons ->
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            if (actionButtons.revoke != SwapActionState.Hidden) {
                ButtonPrimaryYellow(
                    modifier = Modifier.weight(1f),
                    title = actionButtons.revoke.title,
                    onClick = onTapRevoke,
                    enabled = actionButtons.revoke is SwapActionState.Enabled
                )
            }

            if (actionButtons.approve != SwapActionState.Hidden) {
                ButtonPrimaryDefault(
                    modifier = Modifier.weight(1f),
                    title = actionButtons.approve.title,
                    onClick = onTapApprove,
                    enabled = actionButtons.approve is SwapActionState.Enabled
                )
                Spacer(Modifier.width(4.dp))
            }

            if (actionButtons.proceed != SwapActionState.Hidden) {
                ButtonPrimaryYellow(
                    modifier = Modifier.weight(1f),
                    title = actionButtons.proceed.title,
                    onClick = onTapProceed,
                    enabled = actionButtons.proceed is SwapActionState.Enabled
                )
            }
        }
    }
}


@Preview
@Composable
fun Preview_ActionButtons() {
    ComposeAppTheme {
        val buttons = SwapButtons(
            SwapActionState.Enabled("Revoke"),
            SwapActionState.Enabled("Approve"),
            SwapActionState.Enabled("Proceed"),
        )
        ActionButtons(buttons, {}, {}, {})
    }
}

@Preview
@Composable
private fun Preview_SwapError() {
    ComposeAppTheme {
        SwapError("Swap Error text")
    }
}

@Preview
@Composable
private fun Preview_SwitchCoinsSection() {
    ComposeAppTheme {
        SwitchCoinsSection(true, {})
    }
}

@Preview
@Composable
private fun Preview_SwapAllowanceSteps() {
    ComposeAppTheme {
        SwapAllowanceSteps(SwapMainModule.ApproveStep.ApproveRequired)
    }
}