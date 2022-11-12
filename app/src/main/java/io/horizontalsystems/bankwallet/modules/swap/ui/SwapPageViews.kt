package io.horizontalsystems.bankwallet.modules.swap.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapButtons
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
    Row(
        modifier = Modifier
            .height(28.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
//        if (showProgressbar) {
//            Box(Modifier.padding(top = 8.dp)) {
//                HSCircularProgressIndicator()
//            }
//        }
        ButtonSecondaryCircle(
            icon = R.drawable.ic_arrow_down_20,
            onClick = onSwitchButtonClick
        )
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10
        )
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
fun ActionButtons(
    buttons: SwapButtons?,
    onTapRevoke: () -> Unit,
    onTapApprove: () -> Unit,
    onTapProceed: () -> Unit,
) {
    buttons?.let { actionButtons ->
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
                ApproveButton(
                    modifier = Modifier.weight(1f),
                    title = actionButtons.approve.title,
                    onClick = onTapApprove,
                    enabled = actionButtons.approve is SwapActionState.Enabled,
                    step = if (actionButtons.proceed != SwapActionState.Hidden) 1 else null
                )
                if (actionButtons.proceed != SwapActionState.Hidden) {
                    Spacer(Modifier.width(8.dp))
                }
            }

            if (actionButtons.proceed != SwapActionState.Hidden) {
                ProceedButton(
                    modifier = Modifier.weight(1f),
                    title = actionButtons.proceed.title,
                    onClick = onTapProceed,
                    enabled = actionButtons.proceed is SwapActionState.Enabled,
                    step = if (actionButtons.approve != SwapActionState.Hidden) 2 else null
                )
            }
        }
    }
}

@Composable
fun ApproveButton(modifier: Modifier, title: String, onClick: () -> Unit, enabled: Boolean, step: Int? = null) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.leah,
            contentColor = ComposeAppTheme.colors.claude,
            disabledBackgroundColor = ComposeAppTheme.colors.steel20,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            step?.let {
                BadgeStepCircle(text = "$it", active = enabled)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)

        },
        enabled = enabled
    )
}

@Composable
fun ProceedButton(modifier: Modifier, title: String, onClick: () -> Unit, enabled: Boolean, step: Int? = null) {
    ButtonPrimary(
        modifier = modifier,
        onClick = onClick,
        buttonColors = ButtonPrimaryDefaults.textButtonColors(
            backgroundColor = ComposeAppTheme.colors.yellowD,
            contentColor = ComposeAppTheme.colors.dark,
            disabledBackgroundColor = ComposeAppTheme.colors.steel20,
            disabledContentColor = ComposeAppTheme.colors.grey50,
        ),
        content = {
            step?.let {
                BadgeStepCircle(text = "$it", active = enabled)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        enabled = enabled
    )
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
