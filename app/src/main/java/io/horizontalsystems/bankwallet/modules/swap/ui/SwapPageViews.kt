package io.horizontalsystems.bankwallet.modules.swap.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapActionState
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.SwapButtons
import io.horizontalsystems.bankwallet.modules.swap.allowance.SwapAllowanceViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BadgeStepCircle
import io.horizontalsystems.bankwallet.ui.compose.components.BoxTyler44
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.HSCircularProgressIndicator
import io.horizontalsystems.bankwallet.ui.compose.components.SecondaryButtonDefaults
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_lucian

@Composable
fun SwapError(modifier: Modifier = Modifier, text: String) {
    TextImportantError(
        modifier = modifier,
        icon = R.drawable.ic_attention_20,
        title = stringResource(R.string.Error),
        text = text
    )
}

@Composable
fun SwapAllowance(
    viewModel: SwapAllowanceViewModel,
    navController: NavController
) {
    val uiState = viewModel.uiState
    val isError = uiState.isError
    val allowanceAmount = uiState.allowance
    val visible = uiState.isVisible

    if (visible) {
        Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            val infoTitle = stringResource(id = R.string.SwapInfo_AllowanceTitle)
            val infoText = stringResource(id = R.string.SwapInfo_AllowanceDescription)
            Row(
                modifier = Modifier.clickable(
                    onClick = {
                        navController.slideFromBottom(
                            R.id.feeSettingsInfoDialog,
                            FeeSettingsInfoDialog.prepareParams(infoTitle, infoText)
                        )
                    },
                    interactionSource = MutableInteractionSource(),
                    indication = null
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                subhead2_grey(text = stringResource(R.string.Swap_Allowance))

                Image(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    painter = painterResource(id = R.drawable.ic_info_20),
                    contentDescription = ""
                )
            }
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


@Composable
fun AvailableBalance(value: String) {
    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        subhead2_grey(text = stringResource(id = R.string.Swap_Balance))
        Spacer(modifier = Modifier.weight(1f))
        subhead2_leah(text = value)
    }
}

@Composable
fun SuggestionsBar(
    modifier: Modifier = Modifier,
    percents: List<Int> = listOf(25, 50, 75, 100),
    onClick: (Int) -> Unit
) {
    Box(modifier = modifier) {
        BoxTyler44(borderTop = true) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                percents.forEach { percent ->
                    ButtonSecondary(
                        onClick = { onClick.invoke(percent) }
                    ) {
                        subhead1_leah(text = "$percent%")
                    }
                }
            }
        }
    }
}

@Composable
fun SingleLineGroup(
    composableItems: List<@Composable () -> Unit>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            composableItems.forEach { composable ->
                composable()
            }
        }
    }
}

@Composable
fun Price(
    primaryPrice: String,
    secondaryPrice: String,
    timeoutProgress: Float,
    expired: Boolean = false
) {
    var showPrimaryPrice by remember { mutableStateOf(true) }

    Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically)
    {
        subhead2_grey(text = stringResource(R.string.Swap_Price))
        Spacer(Modifier.weight(1f))

        ButtonSecondary(
            onClick = { showPrimaryPrice = !showPrimaryPrice },
            buttonColors = SecondaryButtonDefaults.buttonColors(
                backgroundColor = ComposeAppTheme.colors.transparent,
                contentColor = ComposeAppTheme.colors.leah,
                disabledBackgroundColor = ComposeAppTheme.colors.transparent,
                disabledContentColor = ComposeAppTheme.colors.grey50,
            ),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp),
            content = {
                Text(
                    text = if (showPrimaryPrice) primaryPrice else secondaryPrice,
                    maxLines = 1,
                    style = ComposeAppTheme.typography.subhead2,
                    color = if (expired) ComposeAppTheme.colors.grey50 else ComposeAppTheme.colors.leah,
                )
            }
        )
        Box(modifier = Modifier.size(14.5.dp)) {
            CircularProgressIndicator(
                progress = 1f,
                modifier = Modifier.size(14.5.dp),
                color = ComposeAppTheme.colors.steel20,
                strokeWidth = 1.5.dp
            )
            CircularProgressIndicator(
                progress = timeoutProgress,
                modifier = Modifier
                    .size(14.5.dp)
                    .scale(scaleX = -1f, scaleY = 1f),
                color = ComposeAppTheme.colors.jacob,
                strokeWidth = 1.5.dp
            )
        }
    }
}

@Composable
fun SwitchCoinsSection(onSwitchButtonClick: () -> Unit) {
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
                    step = if (actionButtons.proceed != SwapActionState.Hidden) 1 else null,
                    showProgress = actionButtons.approve.showProgress
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
fun ApproveButton(modifier: Modifier, title: String, onClick: () -> Unit, enabled: Boolean, step: Int? = null, showProgress: Boolean) {
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
            Row(
                modifier = Modifier.weight(9f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showProgress) {
                    HSCircularProgressIndicator()
                } else {
                    step?.let {
                        val background = if (enabled) ComposeAppTheme.colors.claude else ComposeAppTheme.colors.steel20
                        val textColor = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey
                        BadgeStepCircle(text = "$it", background = background, textColor = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (showProgress || step != null) {
                Row(modifier = Modifier.weight(1f)) {}
            }
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
            Row(
                modifier = Modifier.weight(9f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                step?.let {
                    val background = if (enabled) ComposeAppTheme.colors.dark else ComposeAppTheme.colors.steel20
                    val textColor = if (enabled) ComposeAppTheme.colors.yellowD else ComposeAppTheme.colors.grey
                    BadgeStepCircle(text = "$it", background = background, textColor = textColor)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (step != null) {
                Row(modifier = Modifier.weight(1f)) {}
            }
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
        SwapError(text = "Swap Error text")
    }
}

@Preview
@Composable
private fun Preview_SwitchCoinsSection() {
    ComposeAppTheme {
        SwitchCoinsSection {}
    }
}
