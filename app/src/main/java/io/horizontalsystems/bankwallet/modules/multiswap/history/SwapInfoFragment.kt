package io.horizontalsystems.bankwallet.modules.multiswap.history

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFee
import io.horizontalsystems.bankwallet.modules.nav3.NavController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.HsImageCircle
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightControlsButtonText
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
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

@Composable
fun SwapInfoScreen(recordId: Int, navController: NavController) {
    val viewModel = viewModel<SwapInfoViewModel>(
        key = recordId.toString(),
        factory = SwapInfoViewModel.Factory(recordId),
    )
    val uiState = viewModel.uiState
    val view = LocalView.current
    val leah = ComposeAppTheme.colors.leah

    HSScaffold(
        title = stringResource(R.string.SwapInfo_Title),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            VSpacer(12.dp)

            // Token pair card
            Box(
                contentAlignment = Alignment.Center,
            ) {
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
                HsDivider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .background(ComposeAppTheme.colors.lawrence),
                    painter = painterResource(R.drawable.ic_arrow_down_20),
                    tint = ComposeAppTheme.colors.grey,
                    contentDescription = null,
                )
            }

            VSpacer(16.dp)

            // Details card
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
                    .padding(vertical = 8.dp),
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
            }

            VSpacer(24.dp)


            subheadSB_grey(
                text = stringResource(R.string.SwapInfo_StatusTitle),
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            VSpacer(12.dp)

            SwapStatusSteps(
                status = uiState.status,
                isSingleChain = uiState.isSingleChain,
                depositingTxUrl = uiState.depositingTxUrl,
                swappingTxUrl = uiState.swappingTxUrl,
                sendingTxUrl = uiState.sendingTxUrl,
            )

            VSpacer(32.dp)
        }
    }
}

@Composable
private fun SwapStatusSteps(status: SwapStatus, isSingleChain: Boolean, depositingTxUrl: String?, swappingTxUrl: String?, sendingTxUrl: String?) {
    val context = LocalContext.current
    val normalSteps = listOf(
        stringResource(R.string.SwapInfo_StatusDepositing),
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusSending),
    )
    val refundedSteps = listOf(
        stringResource(R.string.SwapInfo_StatusDepositing),
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusRefunded),
    )
    val singleChainNormalSteps = listOf(
        stringResource(R.string.SwapInfo_StatusSwapping),
    )
    val singleChainFailedSteps = listOf(
        stringResource(R.string.SwapInfo_StatusSwapping),
        stringResource(R.string.SwapInfo_StatusFailed),
    )
    val viewLabel = stringResource(R.string.Button_View)

    val steps: List<String>
    val activeIndex: Int
    val failedIndex: Int?

    if (isSingleChain) {
        when (status) {
            SwapStatus.Completed -> {
                steps = singleChainNormalSteps
                activeIndex = steps.size
                failedIndex = null
            }

            SwapStatus.Failed -> {
                steps = singleChainFailedSteps
                activeIndex = -1
                failedIndex = 0
            }

            else -> {
                steps = singleChainNormalSteps
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

            SwapStatus.Failed -> {
                steps = normalSteps
                activeIndex = -1
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

    val green = ComposeAppTheme.colors.remus
    val blade = ComposeAppTheme.colors.blade

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp)
    ) {
        steps.forEachIndexed { index, label ->
            val isFailed = failedIndex == index
            val isDone = activeIndex > index
            val isActive = activeIndex == index
            val isFirst = index == 0
            val isLast = index == steps.lastIndex
            val stepUrl: String? = when (index) {
                0 -> depositingTxUrl
                1 if steps.size > 2 -> swappingTxUrl
                2 if steps.size == 3 -> sendingTxUrl
                else -> null
            }
            val showView = stepUrl != null && (isDone || isActive || isFailed)
            val connectorColor = if (isDone) green else blade

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .then(if (showView) Modifier.clickable {
                        LinkHelper.openLinkInAppBrowser(
                            context,
                            stepUrl
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
                            .background(if (isFirst) Color.Transparent else connectorColor)
                    )
                    StepIndicator(isActive = isActive, isDone = isDone, isFailed = isFailed)
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(if (isLast) Color.Transparent else connectorColor)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    CellMiddleInfoTextIcon(
                        text = label.hs(
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

private fun String.shortenAddress(): String {
    return if (length > 12) take(6) + "..." + takeLast(6) else this
}
