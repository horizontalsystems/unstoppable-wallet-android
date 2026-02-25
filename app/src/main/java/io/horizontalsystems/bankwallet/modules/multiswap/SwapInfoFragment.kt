package io.horizontalsystems.bankwallet.modules.multiswap

import android.os.Parcelable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class SwapInfoFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            SwapInfoScreen(recordId = input.recordId, navController = navController)
        }
    }

    @Parcelize
    data class Input(val recordId: Int) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwapInfoScreen(recordId: Int, navController: NavController) {
    val viewModel = viewModel<SwapInfoViewModel>(
        key = recordId.toString(),
        factory = SwapInfoViewModel.Factory(recordId),
    )
    val uiState = viewModel.uiState
    var showStatusSheet by remember { mutableStateOf(false) }
    val view = LocalView.current
    val leah = ComposeAppTheme.colors.leah

    HSScaffold(
        title = stringResource(R.string.SwapHistory_Title),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            VSpacer(12.dp)

            // Token pair card
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence),
            ) {
                CellPrimary(
                    left = {
                        HsImageCircle(
                            modifier = Modifier.size(32.dp),
                            url = uiState.tokenInImageUrl,
                            placeholder = R.drawable.coin_placeholder,
                        )
                    },
                    middle = {
                        CellMiddleInfo(
                            title = uiState.tokenInCode.hs,
                            subtitle = uiState.tokenInBadge?.hs,
                        )
                    },
                    right = {
                        CellRightInfo(
                            titleSubheadSb = uiState.amountIn.hs,
                            subtitle = uiState.fiatAmountIn?.hs,
                        )
                    },
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    HsDivider()
                    Icon(
                        modifier = Modifier
                            .size(20.dp)
                            .background(ComposeAppTheme.colors.lawrence),
                        painter = painterResource(R.drawable.ic_arrow_down_20),
                        tint = ComposeAppTheme.colors.grey,
                        contentDescription = null,
                    )
                }
                CellPrimary(
                    left = {
                        HsImageCircle(
                            modifier = Modifier.size(32.dp),
                            url = uiState.tokenOutImageUrl,
                            placeholder = R.drawable.coin_placeholder,
                        )
                    },
                    middle = {
                        CellMiddleInfo(
                            title = uiState.tokenOutCode.hs,
                            subtitle = uiState.tokenOutBadge?.hs,
                        )
                    },
                    right = {
                        CellRightInfo(
                            titleSubheadSb = (uiState.amountOut ?: "---").hs,
                            subtitle = uiState.fiatAmountOut?.hs,
                        )
                    },
                )
            }

            VSpacer(12.dp)

            // Details card
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence),
            ) {
                // Provider
                CellSecondary(
                    middle = {
                        CellMiddleInfoTextIcon(text = stringResource(R.string.SwapInfo_Provider).hs)
                    },
                    right = {
                        CellRightInfoTextIcon(text = uiState.providerName.hs(color = leah))
                    },
                )
                // Date
                CellSecondary(
                    middle = {
                        CellMiddleInfoTextIcon(text = stringResource(R.string.TransactionInfo_Date).hs)
                    },
                    right = {
                        CellRightInfoTextIcon(text = uiState.formattedDate.hs(color = leah))
                    },
                )
                // Status (clickable → opens bottom sheet)
                CellSecondary(
                    onClick = { /*showStatusSheet = true*/ },
                    middle = {
                        CellMiddleInfoTextIcon(text = stringResource(R.string.TransactionInfo_Status).hs)
                    },
                    right = {
                        StatusRightSlot(status = uiState.status)
                    },
                )
                // Recipient
                uiState.recipientAddress?.let { address ->
                    CellSecondary(
                        middle = {
                            CellMiddleInfoTextIcon(text = stringResource(R.string.Swap_Recipient).hs)
                        },
                        right = {
                            CellRightControlsButtonText(
                                subtitle = address.shortenAddress().hs(color = leah),
                                icon = painterResource(R.drawable.copy_filled_24),
                                iconTint = leah,
                                onIconClick = {
                                    TextHelper.copyText(address)
                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                },
                            )
                        },
                    )
                }
                // Source address
                uiState.sourceAddress?.let { address ->
                    CellSecondary(
                        middle = {
                            CellMiddleInfoTextIcon(text = stringResource(R.string.SwapInfo_SourceAddress).hs)
                        },
                        right = {
                            CellRightControlsButtonText(
                                subtitle = address.shortenAddress().hs(color = leah),
                                icon = painterResource(R.drawable.copy_filled_24),
                                iconTint = leah,
                                onIconClick = {
                                    TextHelper.copyText(address)
                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                },
                            )
                        },
                    )
                }
                // Fee
                uiState.fee?.let { fee ->
                    CellSecondary(
                        middle = {
                            CellMiddleInfoTextIcon(text = stringResource(R.string.TransactionInfo_Fee).hs)
                        },
                        right = {
                            CellRightInfoTextIcon(text = fee.hs(color = leah))
                        },
                    )
                }
            }

            VSpacer(32.dp)
        }
    }

    if (showStatusSheet) {
        BottomSheetContent(
            onDismissRequest = { showStatusSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = stringResource(R.string.Button_Done),
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showStatusSheet = false },
                )
            }
        ) {
            BottomSheetHeaderV3(title = stringResource(R.string.SwapInfo_StatusTitle))
            subhead2_grey(
                text = stringResource(R.string.SwapInfo_StatusDescription),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
            SwapStatusSteps(status = uiState.status)
            subhead2_grey(
                text = stringResource(R.string.SwapInfo_CrossChainNote),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun StatusRightSlot(status: SwapStatus) {
    val leah = ComposeAppTheme.colors.leah

    val isSpinning = status == SwapStatus.Depositing ||
            status == SwapStatus.Swapping ||
            status == SwapStatus.Sending

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = status.displayName(),
            style = ComposeAppTheme.typography.subheadSB,
            color = leah,
        )

        if (isSpinning) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ComposeAppTheme.colors.leah,
                backgroundColor = ComposeAppTheme.colors.andy,
                strokeWidth = 2.dp
            )
        } else {
            when (status) {
                SwapStatus.Completed -> Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_done_filled_20),
                    tint = ComposeAppTheme.colors.remus,
                    contentDescription = null,
                )

                SwapStatus.Failed -> Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_warning_filled_20),
                    tint = ComposeAppTheme.colors.redL,
                    contentDescription = null,
                )

                SwapStatus.Refunded -> Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_arrow_return_20),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun SwapStatusSteps(status: SwapStatus) {
    val steps = listOf(
        stringResource(R.string.SwapInfo_StepDepositing),
        stringResource(R.string.SwapInfo_StepSwap),
        stringResource(R.string.SwapInfo_StepSend),
        stringResource(R.string.SwapInfo_StepComplete),
    )

    val activeIndex = when (status) {
        SwapStatus.Depositing -> 0
        SwapStatus.Swapping -> 1
        SwapStatus.Sending -> 2
        SwapStatus.Completed -> steps.size
        SwapStatus.Refunded, SwapStatus.Failed -> -1
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence),
    ) {
        steps.forEachIndexed { index, label ->
            val isDone = activeIndex > index
            val isActive = activeIndex == index
            CellSecondary(
                left = { StepIndicator(isActive = isActive, isDone = isDone) },
                middle = {
                    CellMiddleInfoTextIcon(
                        text = label.hs(
                            color = if (isActive) ComposeAppTheme.colors.leah else null
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun StepIndicator(isActive: Boolean, isDone: Boolean) {
    val leah = ComposeAppTheme.colors.leah
    val andy = ComposeAppTheme.colors.andy
    val grey = ComposeAppTheme.colors.grey

    val rotate by if (isActive) {
        rememberInfiniteTransition(label = "step_spinner").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing)
            ),
            label = "step_rotate",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = Modifier
            .size(20.dp)
            .drawBehind {
                if (isActive) {
                    inset(-2.dp.toPx()) {
                        drawArc(
                            color = andy,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        )
                        rotate(degrees = rotate) {
                            drawArc(
                                color = leah,
                                startAngle = 0f,
                                sweepAngle = -120f,
                                useCenter = false,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when {
            isDone -> Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_done_filled_20),
                tint = ComposeAppTheme.colors.remus,
                contentDescription = null,
            )

            !isActive -> Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(1.5.dp, grey, CircleShape),
            )
        }
    }
}

private fun SwapStatus.displayName(): String = when (this) {
    SwapStatus.Depositing -> "Depositing"
    SwapStatus.Swapping -> "Swapping"
    SwapStatus.Sending -> "Sending"
    SwapStatus.Completed -> "Completed"
    SwapStatus.Refunded -> "Refunded"
    SwapStatus.Failed -> "Failed"
}

private fun String.shortenAddress(): String {
    return if (length > 12) take(6) + "..." + takeLast(6) else this
}
