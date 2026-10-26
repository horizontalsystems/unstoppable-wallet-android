package io.horizontalsystems.walletkit.modules.multiswap.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.walletkit.modules.multiswap.SplitAddressRow
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.ui.compose.components.HsImageCircle
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_jacob
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_leah
import io.horizontalsystems.walletkit.ui.compose.components.subheadSB_grey
import io.horizontalsystems.walletkit.ui.helpers.LinkHelper
import io.horizontalsystems.walletkit.ui.helpers.TextHelper
import io.horizontalsystems.walletkit.uiv3.components.AlertCard
import io.horizontalsystems.walletkit.uiv3.components.AlertFormat
import io.horizontalsystems.walletkit.uiv3.components.AlertType
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.walletkit.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.walletkit.uiv3.components.cell.CellSecondary
import io.horizontalsystems.walletkit.uiv3.components.cell.hs
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SwapInfoPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        SwapInfoScreen(recordId = input.recordId, navigation = navigation)
    }

    @Serializable
    data class Input(val recordId: Int)
}

@Composable
fun SwapInfoScreen(recordId: Int, navigation: HSNavigation) {
    val viewModel = viewModel<SwapInfoViewModel>(
        key = recordId.toString(),
        factory = SwapInfoViewModel.Factory(recordId),
    )
    val uiState = viewModel.uiState

    HSScaffold(
        title = operationTitle(uiState.operation),
        onBack = navigation::removeLastOrNull,
        bottomBar = {
            if (uiState.status == SwapStatus.ActionRequired) {
                val coroutineScope = rememberCoroutineScope()
                var refundLoading by remember { mutableStateOf(false) }
                ButtonsGroupWithShade {
                    HSButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        title = stringResource(R.string.SwapInfo_RequestRefund),
                        variant = ButtonVariant.Primary,
                        loadingIndicator = refundLoading,
                        enabled = !refundLoading,
                        onClick = {
                            refundLoading = true
                            coroutineScope.launch {
                                try {
                                    viewModel.prepareRefundData()?.let { data ->
                                        navigation.slideFromBottom(
                                            RequestRefundSheet(RequestRefundSheet.Input(data))
                                        )

                                    }
                                } finally {
                                    refundLoading = false
                                }
                            }
                        },
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            VSpacer(12.dp)

            when (uiState.operation) {
                SwapOperation.Swap -> SwapInfoContent(uiState, navigation)
                SwapOperation.PrivateSend -> PrivateSendInfoContent(uiState, navigation)
                SwapOperation.CrossPay -> CrossPayInfoContent(uiState, navigation)
                SwapOperation.SplitSwap -> SplitSwapInfoContent(uiState, navigation)
            }

            VSpacer(24.dp)

            subheadSB_grey(
                text = stringResource(R.string.SwapInfo_StatusTitle),
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            VSpacer(12.dp)

            if (uiState.operation == SwapOperation.SplitSwap) {
                SplitSwapStatusSteps(uiState)
            } else {
                SwapStatusSteps(
                    status = uiState.status,
                    tokenInCode = uiState.tokenInCode,
                    tokenOutCode = uiState.tokenOutCode,
                    isSingleTransactionSwap = uiState.isSingleTransactionSwap,
                    depositingTxUrl = uiState.depositingTxUrl,
                    swappingTxUrl = uiState.swappingTxUrl,
                    sendingTxUrl = uiState.sendingTxUrl,
                )
            }

            if (uiState.status == SwapStatus.ActionRequired) {
                VSpacer(16.dp)
                AlertCard(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    format = AlertFormat.Structured,
                    type = AlertType.Critical,
                    titleCustom = stringResource(R.string.SwapInfo_SwapStoppedByProvider),
                    text = stringResource(R.string.SwapInfo_SwapStoppedByProviderDescription),
                )
            }

            VSpacer(32.dp)
        }
    }
}

@Composable
private fun SwapInfoContent(uiState: SwapInfoUiState, navigation: HSNavigation) {
    val view = LocalView.current
    val leah = ComposeAppTheme.colors.leah

    Column {
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
                        alternativeUrl = uiState.tokenInAlternativeImageUrl,
                        placeholder = R.drawable.coin_placeholder,
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = uiState.tokenInCode.hs,
                        subtitle = (uiState.tokenInBadge
                            ?: stringResource(id = R.string.CoinPlatforms_Native)).hs,
                    )
                },
                right = {
                    CellRightInfo(
                        titleSubheadSb = uiState.amountIn.hs,
                        subtitle = uiState.fiatAmountIn?.hs,
                    )
                },
            )
            DividerWithArrow()
            CellPrimary(
                left = {
                    HsImageCircle(
                        modifier = Modifier.size(32.dp),
                        url = uiState.tokenOutImageUrl,
                        alternativeUrl = uiState.tokenOutAlternativeImageUrl,
                        placeholder = R.drawable.coin_placeholder,
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = uiState.tokenOutCode.hs,
                        subtitle = (uiState.tokenOutBadge
                            ?: stringResource(id = R.string.CoinPlatforms_Native)).hs,
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

        VSpacer(16.dp)

        SwapDetailsCard(uiState, navigation) {
            // Recipient
            uiState.recipientAddress?.let { address ->
                RecipientRow(address = address, view = view)
            }
        }
    }
}

/** The Route, Date and Swap Time rows of an operation's details, followed by [extraRows]. */
@Composable
internal fun SwapDetailsCard(
    uiState: SwapInfoUiState,
    navigation: HSNavigation,
    extraRows: @Composable () -> Unit = {},
) {
    val leah = ComposeAppTheme.colors.leah
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp),
    ) {
        // Provider
        if (uiState.showProvider) {
            CellSecondary(
                middle = {
                    CellMiddleInfoTextIcon(text = stringResource(R.string.SwapInfo_Provider).hs)
                },
                right = {
                    CellRightInfoTextIcon(text = uiState.providerName.hs(color = leah))
                },
            )
        }
        // Date
        CellSecondary(
            middle = {
                CellMiddleInfoTextIcon(text = stringResource(R.string.TransactionInfo_Date).hs)
            },
            right = {
                CellRightInfoTextIcon(text = uiState.formattedDate.hs(color = leah))
            },
        )
        // Swap Time
        uiState.swapTime?.let { swapTime ->
            val infoTitle = stringResource(R.string.Swap_SwapTime)
            val infoText = stringResource(R.string.Swap_EstimatedTimeDescription)
            CellSecondary(
                middle = {
                    CellMiddleInfoTextIcon(
                        text = infoTitle.hs,
                        icon = painterResource(R.drawable.ic_info_24),
                        iconTint = ComposeAppTheme.colors.grey,
                        onIconClick = {
                            navigation.slideFromBottom(
                                SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText, R.drawable.ic_circle_clock_24))
                            )
                        },
                    )
                },
                right = {
                    CellRightInfoTextIcon(
                        text = swapTime.hs(color = leah)
                    )
                },
            )
        }
        extraRows()
    }
}

/**
 * A split swap: the deposit, then each output with its share, what it received and where.
 * Every output has its own stage, so the status list below carries one swap and payout step
 * per output after the shared deposit.
 */
@Composable
private fun SplitSwapInfoContent(uiState: SwapInfoUiState, navigation: HSNavigation) {
    val view = LocalView.current
    val leah = ComposeAppTheme.colors.leah

    Column {
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
                        alternativeUrl = uiState.tokenInAlternativeImageUrl,
                        placeholder = R.drawable.coin_placeholder,
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = uiState.tokenInCode.hs,
                        subtitle = (uiState.tokenInBadge
                            ?: stringResource(id = R.string.CoinPlatforms_Native)).hs,
                    )
                },
                right = {
                    CellRightInfo(
                        titleSubheadSb = uiState.amountIn.hs,
                        subtitle = uiState.fiatAmountIn?.hs,
                    )
                },
            )
            uiState.splitOutputs.orEmpty().forEachIndexed { position, output ->
                if (position == 0) DividerWithArrow() else HsDivider()
                CellPrimary(
                    left = {
                        HsImageCircle(
                            modifier = Modifier.size(32.dp),
                            url = uiState.tokenOutImageUrl,
                            alternativeUrl = uiState.tokenOutAlternativeImageUrl,
                            placeholder = R.drawable.coin_placeholder,
                        )
                    },
                    middle = {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                subhead2_leah(text = uiState.tokenOutCode)
                                subhead2_jacob(text = "${output.percent}%")
                            }
                            subhead2_grey(text = uiState.tokenOutBadge ?: stringResource(id = R.string.CoinPlatforms_Native))
                        }
                    },
                    right = {
                        CellRightInfo(
                            titleSubheadSb = (output.amountOut ?: "---").hs(color = leah),
                            subtitle = output.fiatAmountOut?.hs,
                        )
                    },
                )
                SplitAddressRow(
                    title = output.contactName ?: stringResource(R.string.SplitSwap_Address, output.index + 1),
                    address = output.address,
                    view = view,
                )
            }
        }

        VSpacer(16.dp)

        SwapDetailsCard(uiState, navigation)
    }
}

/**
 * The shared deposit, then "Swap N" and "Receive CODE N" per output from that output's own
 * stage: outputs finish independently, minutes apart, and one may be held while another is paid.
 */
@Composable
private fun SplitSwapStatusSteps(uiState: SwapInfoUiState) {
    val steps = buildList {
        add(
            StatusStepItem(
                label = stringResource(R.string.SwapInfo_StatusDeposit, uiState.tokenInCode),
                state = when (uiState.status) {
                    SwapStatus.Depositing -> StatusStepState.Active
                    // The deposit never arrived: the first step is the one that failed.
                    SwapStatus.Expired -> StatusStepState.Failed
                    else -> StatusStepState.Done
                },
                url = uiState.depositingTxUrl,
            )
        )
        uiState.splitOutputs.orEmpty().forEach { output ->
            val number = output.index + 1
            val swapLabel = stringResource(R.string.SwapInfo_StatusSwapIndexed, number)
            val receiveLabel = stringResource(R.string.SwapInfo_StatusReceiveIndexed, uiState.tokenOutCode, number)
            when (output.status) {
                SwapStatus.Depositing,
                SwapStatus.Expired -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Pending, null))
                    add(StatusStepItem(receiveLabel, StatusStepState.Pending, null))
                }

                SwapStatus.Swapping -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Active, null))
                    add(StatusStepItem(receiveLabel, StatusStepState.Pending, null))
                }

                SwapStatus.Sending -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Done, null))
                    add(StatusStepItem(receiveLabel, StatusStepState.Active, output.txUrl))
                }

                SwapStatus.Completed -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Done, null))
                    add(StatusStepItem(receiveLabel, StatusStepState.Done, output.txUrl))
                }

                SwapStatus.Refunded -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Done, null))
                    add(StatusStepItem(stringResource(R.string.SwapInfo_StatusRefunded), StatusStepState.Done, output.txUrl))
                }

                SwapStatus.Failed,
                SwapStatus.ActionRequired -> {
                    add(StatusStepItem(swapLabel, StatusStepState.Failed, null))
                }
            }
        }
    }

    StatusStepRows(steps)
}

/**
 * The divider between the two rows of a token card with the direction arrow centred on it.
 * The strip itself is only as tall as the line, so the arrow overlaps both rows equally no
 * matter how tall each row is.
 */
@Composable
internal fun DividerWithArrow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .zIndex(1f),
        contentAlignment = Alignment.Center,
    ) {
        HsDivider()
        Icon(
            modifier = Modifier
                .requiredSize(20.dp)
                .background(ComposeAppTheme.colors.lawrence),
            painter = painterResource(R.drawable.ic_arrow_down_20),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null,
        )
    }
}

/** A "Recipient" detail row with a shortened address and a copy action. */
@Composable
internal fun RecipientRow(address: String, view: android.view.View) {
    val leah = ComposeAppTheme.colors.leah
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

@Composable
internal fun SwapStatusSteps(status: SwapStatus, tokenInCode: String, tokenOutCode: String, isSingleTransactionSwap: Boolean, depositingTxUrl: String?, swappingTxUrl: String?, sendingTxUrl: String?) {
    val normalSteps = listOf(
        stringResource(R.string.SwapInfo_StatusDeposit, tokenInCode),
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusReceive, tokenOutCode),
    )
    val refundedSteps = listOf(
        stringResource(R.string.SwapInfo_StatusDeposit, tokenInCode),
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusRefunded),
    )
    val singleTransactionNormalSteps = listOf(
        stringResource(R.string.SwapInfo_StatusSwapping),
    )
    val singleTransactionFailedSteps = listOf(
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusFailed),
    )

    val steps: List<String>
    val activeIndex: Int
    val failedIndex: Int?

    if (isSingleTransactionSwap) {
        when (status) {
            SwapStatus.Completed -> {
                steps = singleTransactionNormalSteps
                activeIndex = steps.size
                failedIndex = null
            }

            SwapStatus.Failed,
            SwapStatus.ActionRequired,
            SwapStatus.Expired -> {
                steps = singleTransactionFailedSteps
                activeIndex = -1
                failedIndex = 1
            }

            else -> {
                steps = singleTransactionNormalSteps
                activeIndex = 0
                failedIndex = null
            }
        }
    } else {
        when (status) {
            SwapStatus.Refunded -> {
                steps = refundedSteps
                activeIndex = steps.size
                failedIndex = null
            }

            SwapStatus.Failed,
            SwapStatus.ActionRequired -> {
                steps = normalSteps.take(2)
                activeIndex = 1
                failedIndex = 1
            }

            // The deposit never arrived: the first step is the one that failed.
            SwapStatus.Expired -> {
                steps = normalSteps.take(1)
                activeIndex = 0
                failedIndex = 0
            }

            SwapStatus.Depositing -> {
                steps = normalSteps
                activeIndex = 0
                failedIndex = null
            }

            SwapStatus.Swapping -> {
                steps = normalSteps
                activeIndex = 1
                failedIndex = null
            }

            SwapStatus.Sending -> {
                steps = normalSteps
                activeIndex = 2
                failedIndex = null
            }

            SwapStatus.Completed -> {
                steps = normalSteps
                activeIndex = steps.size
                failedIndex = null
            }
        }
    }

    StatusStepRows(
        steps.mapIndexed { index, label ->
            StatusStepItem(
                label = label,
                state = when {
                    failedIndex == index -> StatusStepState.Failed
                    activeIndex > index -> StatusStepState.Done
                    activeIndex == index -> StatusStepState.Active
                    else -> StatusStepState.Pending
                },
                url = when (index) {
                    0 -> depositingTxUrl
                    1 if steps.size > 2 -> swappingTxUrl
                    2 if steps.size == 3 -> sendingTxUrl
                    else -> null
                },
            )
        }
    )
}

enum class StatusStepState {
    Pending,
    Active,
    Done,
    Failed,
}

/** One row of a status list: its label, where it stands, and the explorer link when there is one. */
data class StatusStepItem(
    val label: String,
    val state: StatusStepState,
    val url: String?,
)

/** A vertical list of steps joined by connectors, green up to the last completed one. */
@Composable
internal fun StatusStepRows(steps: List<StatusStepItem>) {
    val context = LocalContext.current
    val viewLabel = stringResource(R.string.Button_View)
    val green = ComposeAppTheme.colors.remus
    val blade = ComposeAppTheme.colors.blade

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp)
    ) {
        steps.forEachIndexed { index, step ->
            val isFailed = step.state == StatusStepState.Failed
            val isDone = step.state == StatusStepState.Done
            val isActive = step.state == StatusStepState.Active
            val isFirst = index == 0
            val isLast = index == steps.lastIndex
            val showView = step.url != null && step.state != StatusStepState.Pending
            val isPrevDone = index > 0 && steps[index - 1].state == StatusStepState.Done
            val topConnectorColor = if (isPrevDone) green else blade
            val bottomConnectorColor = if (isDone) green else blade

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .then(if (showView) Modifier.clickable {
                        LinkHelper.openLinkInAppBrowser(
                            context,
                            step.url
                        )
                    } else Modifier)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: connector lines + step indicator
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(if (isFirst) Color.Transparent else topConnectorColor)
                    )
                    StepIndicator(isActive = isActive, isDone = isDone, isFailed = isFailed)
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(if (isLast) Color.Transparent else bottomConnectorColor)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    CellMiddleInfoTextIcon(
                        text = step.label.hs(
                            color = if (isActive || isDone || isFailed) ComposeAppTheme.colors.leah else null
                        )
                    )
                }

                if (showView) {
                    Box(
                        modifier = Modifier.widthIn(max = 200.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        CellRightNavigation(subtitle = viewLabel.hs)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(isActive: Boolean, isDone: Boolean, isFailed: Boolean = false) {
    when {
        isActive -> Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_circle_placeholder_20),
                tint = ComposeAppTheme.colors.blade,
                contentDescription = null,
            )
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ComposeAppTheme.colors.leah,
                backgroundColor = Color.Transparent,
                strokeWidth = 2.dp,
            )
        }

        isFailed -> Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_circle_placeholder_20),
                tint = ComposeAppTheme.colors.blade,
                contentDescription = null,
            )
            Icon(
                modifier = Modifier.size(11.dp),
                painter = painterResource(R.drawable.ic_failed_cross),
                tint = ComposeAppTheme.colors.lucian,
                contentDescription = null,
            )
        }

        isDone -> Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_check_filled_20_no_padding),
            tint = ComposeAppTheme.colors.remus,
            contentDescription = null,
        )

        else -> Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_circle_placeholder_20),
            tint = ComposeAppTheme.colors.blade,
            contentDescription = null,
        )
    }
}

internal fun String.shortenAddress(): String {
    return if (length > 12) take(6) + "..." + takeLast(6) else this
}
