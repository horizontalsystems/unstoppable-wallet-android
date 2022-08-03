package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.iconUrl
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInputRaw
import io.horizontalsystems.bankwallet.modules.hodler.HSHodlerInput
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.snackbar.SnackbarDuration
import kotlinx.coroutines.delay
import java.math.BigDecimal

@Composable
fun SendConfirmationScreen(
    navController: NavController,
    coinMaxAllowedDecimals: Int,
    feeCoinMaxAllowedDecimals: Int,
    fiatMaxAllowedDecimals: Int,
    amountInputType: AmountInputType,
    rate: CurrencyValue?,
    feeCoinRate: CurrencyValue?,
    sendResult: SendResult?,
    coin: Coin,
    feeCoin: Coin,
    amount: BigDecimal,
    address: Address,
    fee: BigDecimal,
    lockTimeInterval: LockTimeInterval?,
    memo: String?,
    onClickSend: () -> Unit
) {
    val view = LocalView.current
    when (sendResult) {
        SendResult.Sending -> {
            HudHelper.showInProcessMessage(
                view,
                R.string.Send_Sending,
                SnackbarDuration.INDEFINITE
            )
        }
        SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.Send_Success,
                SnackbarDuration.LONG
            )
        }
        is SendResult.Failed -> {
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
        }
        null -> Unit
    }

    LaunchedEffect(sendResult) {
        if (sendResult == SendResult.Sent) {
            delay(1200)
            navController.popBackStack(R.id.sendXFragment, true)
        }
    }

    ComposeAppTheme {
        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = TranslatableString.ResString(R.string.Send_Confirmation_Title),
                navigationIcon = {
                    HsIconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back button",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = {
                            navController.popBackStack(R.id.sendXFragment, true)
                        }
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 106.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HSSectionRounded {
                        CellSingleLineLawrence {
                            SectionTitleCell(
                                stringResource(R.string.Send_Confirmation_YouSend),
                                coin.name,
                                R.drawable.ic_arrow_up_right_12
                            )
                        }
                        CellSingleLineLawrence(borderTop = true) {
                            val coinAmount = App.numberFormatter.formatCoinFull(
                                amount,
                                coin.code,
                                coinMaxAllowedDecimals
                            )

                            val currencyAmount = rate?.let { rate ->
                                rate.copy(value = amount.times(rate.value))
                                    .getFormattedFull()
                            }

                            ConfirmAmountCell(currencyAmount, coinAmount, coin)
                        }
                        CellSingleLineLawrence(borderTop = true) {
                            AddressCell(address.hex)
                        }
                        if (lockTimeInterval != null) {
                            CellSingleLineLawrence(borderTop = true) {
                                HSHodlerInput(lockTimeInterval = lockTimeInterval)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HSSectionRounded {
                        CellSingleLineLawrence {
                            HSFeeInputRaw(
                                coinCode = feeCoin.code,
                                coinDecimal = feeCoinMaxAllowedDecimals,
                                fiatDecimal = fiatMaxAllowedDecimals,
                                fee = fee,
                                amountInputType = amountInputType,
                                rate = feeCoinRate,
                                enabled = false,
                                onClick = {}
                            )
                        }
                        if (!memo.isNullOrBlank()) {
                            CellSingleLineLawrence(borderTop = true) {
                                MemoCell(memo)
                            }
                        }
                    }
                }

                SendButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    sendResult = sendResult,
                    onClickSend = onClickSend
                )
            }
        }
    }
}

@Composable
private fun SendButton(modifier: Modifier, sendResult: SendResult?, onClickSend: () -> Unit) {
    when (sendResult) {
        SendResult.Sending -> {
            ButtonPrimaryYellow(
                modifier = modifier,
                title = stringResource(R.string.Send_Sending),
                onClick = { },
                enabled = false
            )
        }
        SendResult.Sent -> {
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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinImage(
            iconUrl = coin.iconUrl,
            placeholder = R.drawable.coin_placeholder,
            modifier = Modifier.size(24.dp)
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
fun AddressCell(address: String) {
    val clipboardManager = LocalClipboardManager.current
    val view = LocalView.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(text = stringResource(R.string.Send_Confirmation_To))
        Spacer(Modifier.weight(1f))
        ButtonSecondaryDefault(
            modifier = Modifier
                .padding(start = 8.dp),
            title = address.shorten(),
            onClick = {
                clipboardManager.setText(AnnotatedString(address))
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        )
    }
}

@Composable
fun MemoCell(value: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        subhead2_grey(
            modifier = Modifier.padding(end = 16.dp),
            text = stringResource(R.string.Send_Confirmation_HintMemo),
        )
        Spacer(Modifier.weight(1f))
        subhead1Italic_leah(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
