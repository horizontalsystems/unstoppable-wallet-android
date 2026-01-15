package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.fee.FeeItem
import io.horizontalsystems.bankwallet.modules.multiswap.QuoteInfoRow
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.AddressCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import java.math.BigDecimal

@Composable
fun SendConfirmationScreen(
    navController: NavController,
    coinMaxAllowedDecimals: Int,
    feeCoinMaxAllowedDecimals: Int,
    rate: CurrencyValue?,
    feeCoinRate: CurrencyValue?,
    sendResult: SendResult?,
    token: Token,
    feeCoin: Coin,
    amount: BigDecimal,
    address: Address?,
    contact: Contact?,
    fee: BigDecimal?,
    memo: String?,
    onClickSend: () -> Unit,
    sendEntryPointDestId: Int,
    title: String? = null,
    additionalFields: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val closeUntilDestId = if (sendEntryPointDestId == 0) {
        R.id.sendXFragment
    } else {
        sendEntryPointDestId
    }
    val view = LocalView.current
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }

        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.Send_Success,
                SnackbarDuration.LONG
            )
        }

        is SendResult.Failed -> {
            HudHelper.showErrorMessage(
                view,
                sendResult.caution.getDescription() ?: sendResult.caution.getString()
            )
        }

        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult is SendResult.Sent) {
            delay(1200)
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        if (sendResult is SendResult.Sent) {
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    HSScaffold(
        title = title ?: stringResource(R.string.Send_Confirmation_Title),
        onBack = navController::popBackStack,
        bottomBar = {
            ButtonsGroupWithShade {
                SendButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    sendResult = sendResult,
                    onClickSend = {
                        onClickSend()

                        stat(page = StatPage.SendConfirmation, event = StatEvent.Send)
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 106.dp)
        ) {
            VSpacer(16.dp)
            ConfirmationTopSection(
                token = token,
                amount = amount,
                coinMaxAllowedDecimals = coinMaxAllowedDecimals,
                rate = rate,
                address = address,
                contact = contact,
            )

            ConfirmationBottomSection(
                feeCoin = feeCoin,
                feeCoinMaxAllowedDecimals = feeCoinMaxAllowedDecimals,
                fee = fee,
                feeCoinRate = feeCoinRate,
                navController = navController,
                memo = memo,
                additionalFields = additionalFields
            )
        }
    }
}

@Composable
fun ConfirmationBottomSection(
    feeCoin: Coin,
    feeCoinMaxAllowedDecimals: Int,
    fee: BigDecimal?,
    feeCoinRate: CurrencyValue?,
    navController: NavController,
    memo: String?,
    customFeeInfo: String? = null,
    additionalFields: (@Composable ColumnScope.() -> Unit)? = null,
) {
    var formattedFee by remember { mutableStateOf<FeeItem?>(null) }

    LaunchedEffect(fee, feeCoinRate) {
        formattedFee = getFormattedFee(fee, feeCoinRate, feeCoin.code, feeCoinMaxAllowedDecimals)
    }

    VSpacer(16.dp)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp)
    ) {
        additionalFields?.let {
            it()
        }

        if (!memo.isNullOrBlank()) {
            MemoCell(memo)
        }
        DataFieldFeeTemplate(
            navController = navController,
            primary = formattedFee?.primary ?: "---",
            secondary = formattedFee?.secondary,
            title = stringResource(id = R.string.FeeSettings_NetworkFee),
            infoText = customFeeInfo ?: stringResource(id = R.string.FeeSettings_NetworkFee_Info)
        )
    }
}

@Composable
fun ConfirmationTopSection(
    token: Token,
    amount: BigDecimal,
    coinMaxAllowedDecimals: Int,
    rate: CurrencyValue?,
    address: Address?,
    contact: Contact?,
) {
    val coinAmount = App.numberFormatter.formatCoinFull(
        amount,
        null,
        coinMaxAllowedDecimals
    )

    val fiatAmount = rate?.let { rate ->
        rate.copy(value = amount.times(rate.value))
            .getFormattedFull()
    }
    Box {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            CellPrimary(
                left = {
                    CoinImage(
                        token = token,
                        modifier = Modifier.size(32.dp)
                    )
                },
                middle = {
                    CellMiddleInfo(
                        subtitle = token.coin.code.hs(color = ComposeAppTheme.colors.leah),
                        description = (token.badge ?: stringResource(id = R.string.CoinPlatforms_Native)).hs
                    )
                },
                right = {
                    CellRightInfo(
                        titleSubheadSb = coinAmount.hs,
                        description = fiatAmount?.hs
                    )
                }
            )
            address?.let {
                HsDivider()
                AddressCell(
                    address = it.hex,
                    contact = contact?.name
                )
            }
        }
        if (address != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 57.dp) //top cell is 67.dp - iconWidth/2(which is equal 10.dp)
                    .clip(CircleShape)
                    .background(ComposeAppTheme.colors.lawrence)
            )
        }
    }
}

@Composable
fun SendButton(modifier: Modifier, sendResult: SendResult?, onClickSend: () -> Unit) {
    when (sendResult) {
        SendResult.Sending -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Sending),
                onClick = { },
                enabled = false
            )
        }

        is SendResult.Sent -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Success),
                onClick = { },
                enabled = false
            )
        }

        else -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Confirmation_Send_Button),
                onClick = onClickSend,
                enabled = true
            )
        }
    }
}

@Composable
fun ConfirmAmountCell(fiatAmount: String?, coinAmount: String, coin: Coin) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        CoinImage(
            coin = coin,
            modifier = Modifier.size(32.dp)
        )
        subhead2_leah(
            modifier = Modifier.padding(start = 16.dp),
            text = coinAmount,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        subhead1_grey(text = fiatAmount ?: "")
    }
}

@Composable
fun MemoCell(value: String) {
    QuoteInfoRow(
        title = stringResource(R.string.Send_Confirmation_HintMemo),
        value = value.hs(color = ComposeAppTheme.colors.leah),
    )
}

fun getFormattedFee(
    fee: BigDecimal?,
    rate: CurrencyValue?,
    coinCode: String,
    coinDecimal: Int,
): FeeItem? {

    if (fee == null) return null

    val coinAmount = App.numberFormatter.formatCoinFull(fee, coinCode, coinDecimal)
    val currencyAmount = rate?.let {
        it.copy(value = fee.times(it.value)).getFormattedFull()
    }

    return  FeeItem(coinAmount, currencyAmount)
}