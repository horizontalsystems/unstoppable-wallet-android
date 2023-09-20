package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.fee.HSFeeInputRaw
import io.horizontalsystems.bankwallet.modules.hodler.HSHodler
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1Italic_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
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
    blockchainType: BlockchainType,
    coin: Coin,
    feeCoin: Coin,
    amount: BigDecimal,
    address: Address,
    contact: Contact?,
    fee: BigDecimal,
    lockTimeInterval: LockTimeInterval?,
    memo: String?,
    onClickSend: () -> Unit,
    sendEntryPointDestId: Int
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
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    ComposeAppTheme {
        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(R.string.Send_Confirmation_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf()
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
                    val topSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            SectionTitleCell(
                                stringResource(R.string.Send_Confirmation_YouSend),
                                coin.name,
                                R.drawable.ic_arrow_up_right_12
                            )
                        }
                        add {
                            val coinAmount = App.numberFormatter.formatCoinFull(
                                amount,
                                coin.code,
                                coinMaxAllowedDecimals
                            )

                            val currencyAmount = rate?.let { rate ->
                                rate.copy(value = amount.times(rate.value))
                                    .getFormattedFull()
                            }

                            ConfirmAmountCell(currencyAmount, coinAmount, coin.imageUrl)
                        }
                        add {
                            TransactionInfoAddressCell(
                                title = stringResource(R.string.Send_Confirmation_To),
                                value = address.hex,
                                showAdd = contact == null,
                                blockchainType = blockchainType,
                                navController = navController
                            )
                        }
                        contact?.let {
                            add {
                                TransactionInfoContactCell(name = contact.name)
                            }
                        }
                        if (lockTimeInterval != null) {
                            add {
                                HSHodler(lockTimeInterval = lockTimeInterval)
                            }
                        }
                    }

                    CellUniversalLawrenceSection(topSectionItems)

                    Spacer(modifier = Modifier.height(28.dp))

                    val bottomSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            HSFeeInputRaw(
                                coinCode = feeCoin.code,
                                coinDecimal = feeCoinMaxAllowedDecimals,
                                fee = fee,
                                amountInputType = amountInputType,
                                rate = feeCoinRate,
                                navController = navController
                            )
                        }
                        if (!memo.isNullOrBlank()) {
                            add {
                                MemoCell(memo)
                            }
                        }
                    }

                    CellUniversalLawrenceSection(bottomSectionItems)
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
fun ConfirmAmountCell(fiatAmount: String?, coinAmount: String, iconUrl: String?) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        CoinImage(
            iconUrl = iconUrl,
            placeholder = R.drawable.coin_placeholder,
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
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
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
