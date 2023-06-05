package cash.p.terminal.modules.send.tron

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.evmfee.FeeSettingsInfoDialog
import cash.p.terminal.modules.fee.HSFeeInputRaw
import cash.p.terminal.modules.fee.HSFeeInputRawWithViewState
import cash.p.terminal.modules.send.ConfirmAmountCell
import cash.p.terminal.modules.send.MemoCell
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui.compose.components.HSpacer
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.HsIconButton
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.RowUniversal
import cash.p.terminal.ui.compose.components.SectionTitleCell
import cash.p.terminal.ui.compose.components.TextImportantError
import cash.p.terminal.ui.compose.components.TextImportantWarning
import cash.p.terminal.ui.compose.components.TransactionInfoAddressCell
import cash.p.terminal.ui.compose.components.TransactionInfoContactCell
import cash.p.terminal.ui.compose.components.subhead1_leah
import cash.p.terminal.ui.compose.components.subhead2_grey
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

@Composable
fun SendTronConfirmationScreen(
    navController: NavController,
    sendViewModel: SendTronViewModel,
    amountInputModeViewModel: AmountInputModeViewModel
) {
    val confirmationData = sendViewModel.confirmationData ?: return

    val uiState = sendViewModel.uiState
    val sendEnabled = uiState.sendEnabled
    val feeViewState = uiState.feeViewState
    val cautions = uiState.cautions

    val coinMaxAllowedDecimals = sendViewModel.coinMaxAllowedDecimals
    val feeCoinMaxAllowedDecimals = sendViewModel.feeTokenMaxAllowedDecimals
    val amountInputType = amountInputModeViewModel.inputType
    val rate = sendViewModel.coinRate
    val feeCoinRate = sendViewModel.feeCoinRate
    val sendResult = sendViewModel.sendResult
    val coin = confirmationData.coin
    val blockchainType = sendViewModel.blockchainType
    val feeCoin = confirmationData.feeCoin
    val amount = confirmationData.amount
    val address = confirmationData.address
    val contact = confirmationData.contact
    val fee = confirmationData.fee
    val activationFee = confirmationData.activationFee
    val resourcesConsumed = confirmationData.resourcesConsumed
    val memo = confirmationData.memo
    val onClickSend = sendViewModel::onClickSend

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
                    HsBackButton(onClick = { navController.popBackStack() })
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

                            ConfirmAmountCell(currencyAmount, coinAmount, coin)
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
                    }

                    CellUniversalLawrenceSection(topSectionItems)

                    Spacer(modifier = Modifier.height(16.dp))

                    val bottomSectionItems = buildList<@Composable () -> Unit> {
                        add {
                            HSFeeInputRawWithViewState(
                                coinCode = feeCoin.code,
                                coinDecimal = feeCoinMaxAllowedDecimals,
                                fee = fee,
                                viewState = feeViewState,
                                amountInputType = amountInputType,
                                rate = feeCoinRate,
                                navController = navController
                            )
                        }

                        if (feeViewState == ViewState.Success) {
                            activationFee?.let {
                                add {
                                    HSFeeInputRaw(
                                        title = stringResource(R.string.FeeInfo_TronActivationFee_Title),
                                        info = stringResource(R.string.FeeInfo_TronActivationFee_Description),
                                        coinCode = feeCoin.code,
                                        coinDecimal = feeCoinMaxAllowedDecimals,
                                        fee = it,
                                        amountInputType = amountInputType,
                                        rate = feeCoinRate,
                                        navController = navController
                                    )
                                }
                            }

                            resourcesConsumed?.let {
                                add {
                                    ResourcesConsumed(
                                        title = stringResource(R.string.FeeInfo_TronResourcesConsumed_Title),
                                        value = it,
                                        info = stringResource(R.string.FeeInfo_TronResourcesConsumed_Description),
                                        navController = navController
                                    )
                                }
                            }
                        }

                        if (!memo.isNullOrBlank()) {
                            add {
                                MemoCell(memo)
                            }
                        }
                    }

                    CellUniversalLawrenceSection(bottomSectionItems)

                    if (cautions.isNotEmpty()) {
                        Cautions(cautions)
                    }
                }

                SendButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),

                    sendResult = sendResult,
                    onClickSend = onClickSend,
                    enabled = sendEnabled
                )
            }
        }

    }
}

@Composable
private fun SendButton(modifier: Modifier, sendResult: SendResult?, onClickSend: () -> Unit, enabled: Boolean) {
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
                enabled = enabled
            )
        }
    }
}

@Composable
private fun ResourcesConsumed(
    title: String,
    value: String,
    info: String,
    navController: NavController
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        subhead2_grey(text = title)
        Spacer(modifier = Modifier.width(8.dp))
        HsIconButton(
            modifier = Modifier.size(20.dp),
            onClick = {
                navController.slideFromBottom(
                    R.id.feeSettingsInfoDialog, FeeSettingsInfoDialog.prepareParams(title, info)
                )
            }
        ) {
            Image(
                painter = painterResource(R.drawable.ic_info_20),
                contentDescription = null
            )
        }

        HSpacer(16.dp)
        subhead1_leah(
            modifier = Modifier.weight(1f),
            text = value,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
private fun Cautions(cautions: List<HSCaution>) {
    Spacer(modifier = Modifier.height(32.dp))

    val modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        cautions.forEach { caution ->

            when (caution.type) {
                HSCaution.Type.Error -> {
                    TextImportantError(
                        modifier = modifier,
                        text = caution.getString(),
                        title = stringResource(R.string.Error),
                        icon = R.drawable.ic_attention_20
                    )
                }

                HSCaution.Type.Warning -> {
                    TextImportantWarning(
                        modifier = modifier,
                        text = caution.getString(),
                        title = stringResource(R.string.Alert_TitleWarning),
                        icon = R.drawable.ic_attention_20
                    )
                }
            }
        }
    }
}
