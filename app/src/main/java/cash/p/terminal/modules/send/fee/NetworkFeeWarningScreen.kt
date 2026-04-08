package cash.p.terminal.modules.send.fee

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.isNative
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.HsIconButton
import cash.p.terminal.ui_compose.components.HsRadioButton
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.SectionUniversalItem
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import java.math.BigDecimal

data class NetworkFeeWarningData(
    val networkName: String,
    val feeAmount: String,
    val balanceThreshold: String,
    val feeCoinCode: String,
)

internal val TRX_WARNING_THRESHOLD = BigDecimal("50")
internal val TON_WARNING_THRESHOLD = BigDecimal("0.5")

internal fun getWarningThreshold(blockchainType: BlockchainType): BigDecimal? {
    return when (blockchainType) {
        BlockchainType.Tron -> TRX_WARNING_THRESHOLD
        BlockchainType.Ton -> TON_WARNING_THRESHOLD
        else -> null
    }
}

private fun shouldShowNetworkFeeWarning(
    blockchainType: BlockchainType,
    tokenType: TokenType,
    feeTokenBalance: BigDecimal?,
    estimatedFee: BigDecimal?,
): Boolean {
    val balance = feeTokenBalance ?: return false
    val fee = estimatedFee ?: return false

    if (tokenType.isNative) return false
    // When balance < fee, no warning needed — the insufficient-balance
    // check elsewhere will block the transaction entirely.
    if (balance < fee) return false

    val threshold = getWarningThreshold(blockchainType) ?: return false
    return balance < threshold
}

fun buildNetworkFeeWarningData(
    blockchainType: BlockchainType,
    tokenType: TokenType,
    feeTokenBalance: BigDecimal?,
    estimatedFee: BigDecimal?,
    feeToken: Token,
): NetworkFeeWarningData? {
    if (!shouldShowNetworkFeeWarning(blockchainType, tokenType, feeTokenBalance, estimatedFee)) {
        return null
    }
    val fee = estimatedFee ?: return null
    val threshold = getWarningThreshold(blockchainType) ?: return null
    val formatter = App.numberFormatter
    return NetworkFeeWarningData(
        networkName = when (blockchainType) {
            BlockchainType.Tron -> "TRON"
            BlockchainType.Ton -> "TON"
            else -> ""
        },
        feeAmount = formatter.formatCoinFull(fee, feeToken.coin.code, feeToken.decimals),
        balanceThreshold = formatter.formatCoinFull(threshold, feeToken.coin.code, feeToken.decimals),
        feeCoinCode = feeToken.coin.code,
    )
}

@Composable
fun NetworkFeeWarningOverlay(
    feeWarningData: NetworkFeeWarningData?,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var currentData by remember { mutableStateOf(feeWarningData) }

    if (feeWarningData != null) {
        currentData = feeWarningData
    }

    val data = currentData ?: return

    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = feeWarningData != null

    if (visibleState.isIdle && !visibleState.currentState) {
        currentData = null
        return
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        BackHandler(onBack = onCancel)
        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NetworkFeeWarningContent(
                networkName = data.networkName,
                feeAmount = data.feeAmount,
                balanceThreshold = data.balanceThreshold,
                feeCoinCode = data.feeCoinCode,
                onConfirm = onConfirm,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun NetworkFeeWarningContent(
    networkName: String,
    feeAmount: String,
    balanceThreshold: String,
    feeCoinCode: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var accepted by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .clip(RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp))
            .background(color = ComposeAppTheme.colors.tyler)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 32.dp, top = 24.dp, end = 32.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_attention_24),
                colorFilter = ColorFilter.tint(ComposeAppTheme.colors.jacob),
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.network_fee_warning_title, networkName),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                maxLines = 1,
                style = ComposeAppTheme.typography.headline2,
                color = ComposeAppTheme.colors.leah,
            )
            HsIconButton(
                modifier = Modifier.size(24.dp),
                onClick = onCancel
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24),
                    tint = ComposeAppTheme.colors.jacob,
                    contentDescription = null,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            VSpacer(12.dp)

            TextImportantWarning(
                text = buildWarningText(networkName, feeAmount, balanceThreshold, feeCoinCode),
            )

            VSpacer(12.dp)
            subhead2_grey(
                text = stringResource(R.string.network_fee_warning_points_header),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            VSpacer(8.dp)

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                SectionUniversalItem {
                    RowUniversal(
                        onClick = { accepted = !accepted },
                    ) {
                        HsRadioButton(
                            selected = accepted,
                            onClick = { accepted = !accepted },
                        )
                        subhead2_grey(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.network_fee_warning_checkbox),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            VSpacer(24.dp)
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Confirm),
                enabled = accepted,
                onClick = onConfirm,
            )
            VSpacer(12.dp)
            ButtonPrimaryTransparent(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Cancel),
                onClick = onCancel,
            )
            VSpacer(32.dp)
        }
    }
}

@Preview
@Composable
private fun NetworkFeeWarningContentPreview() {
    ComposeAppTheme {
        NetworkFeeWarningContent(
            networkName = "TRON",
            feeAmount = "13.3735 TRX",
            balanceThreshold = "50 TRX",
            feeCoinCode = "TRX",
            onConfirm = {},
            onCancel = {},
        )
    }
}

@Composable
private fun buildWarningText(
    networkName: String,
    feeAmount: String,
    balanceThreshold: String,
    feeCoinCode: String,
): AnnotatedString {
    val bold = SpanStyle(fontWeight = FontWeight.Bold)

    val protocolText = stringResource(
        R.string.network_fee_warning_body_protocol, networkName, feeAmount
    )
    val protocolBoldPhrase = stringResource(
        R.string.network_fee_warning_body_protocol_bold, feeAmount
    )
    val balanceText = stringResource(
        R.string.network_fee_warning_body_balance, balanceThreshold
    )
    val balanceBoldPhrase = stringResource(
        R.string.network_fee_warning_body_balance_bold, balanceThreshold
    )
    val riskText = stringResource(R.string.network_fee_warning_body_risk)
    val caseIntro = stringResource(R.string.network_fee_warning_body_case_intro)
    val caseDebited = stringResource(
        R.string.network_fee_warning_body_case_debited, feeCoinCode
    )
    val caseNotCompletedPrefix =
        stringResource(R.string.network_fee_warning_body_case_not_completed_prefix)
    val caseNotCompleted = stringResource(R.string.network_fee_warning_body_case_not_completed)
    val caseNotSentPrefix =
        stringResource(R.string.network_fee_warning_body_case_not_sent_prefix)
    val caseNotSent = stringResource(R.string.network_fee_warning_body_case_not_sent)
    val confirmIntro = stringResource(R.string.network_fee_warning_body_confirm_intro)
    val confirmUnderstand = stringResource(
        R.string.network_fee_warning_body_confirm_understand, networkName
    )
    val confirmRisk = stringResource(R.string.network_fee_warning_body_confirm_risk)
    val confirmAccept = stringResource(R.string.network_fee_warning_body_confirm_accept)

    return buildAnnotatedString {
        // Paragraph 1: protocol + balance + risk
        appendBolding(protocolText, bold, networkName, protocolBoldPhrase)
        append(" ")
        appendBolding(balanceText, bold, balanceBoldPhrase)
        append(" ")
        appendBolding(
            riskText,
            bold,
            stringResource(R.string.network_fee_warning_body_risk_bold)
        )

        // "In this case:" + bold bullets
        append("\n\n")
        append(caseIntro)
        append("\n")

        append("  \u2022 ")
        withStyle(bold) { append(caseDebited) }
        append("\n")

        append("  \u2022 ")
        append(caseNotCompletedPrefix)
        append(" ")
        withStyle(bold) { append(caseNotCompleted) }
        append("\n")

        append("  \u2022 ")
        append(caseNotSentPrefix)
        append(" ")
        withStyle(bold) { append(caseNotSent) }

        // "By continuing..." + regular bullets
        append("\n\n")
        append(confirmIntro)
        append("\n")
        append("  \u2022 ")
        append(confirmUnderstand)
        append("\n")
        append("  \u2022 ")
        append(confirmRisk)
        append("\n")
        append("  \u2022 ")
        append(confirmAccept)
    }
}

/**
 * Appends [text] to the builder, applying [style] to each occurrence of [boldParts].
 */
private fun AnnotatedString.Builder.appendBolding(
    text: String,
    style: SpanStyle,
    vararg boldParts: String,
) {
    if (boldParts.isEmpty()) {
        append(text)
        return
    }

    // Collect all bold ranges sorted by position
    val ranges = boldParts
        .flatMap { part ->
            buildList {
                var start = 0
                while (true) {
                    val idx = text.indexOf(part, start, ignoreCase = true)
                    if (idx < 0) break
                    add(idx to idx + part.length)
                    start = idx + part.length
                }
            }
        }
        .sortedBy { it.first }

    var cursor = 0
    for ((start, end) in ranges) {
        if (start < cursor) continue // overlapping range, skip
        if (start > cursor) {
            append(text.substring(cursor, start))
        }
        withStyle(style) {
            append(text.substring(start, end))
        }
        cursor = end
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}
