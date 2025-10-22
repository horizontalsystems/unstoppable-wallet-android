package io.horizontalsystems.bankwallet.modules.send.tron

import androidx.compose.foundation.Image
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsInfoDialog
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRaw
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRawWithViewState
import io.horizontalsystems.bankwallet.modules.send.ConfirmAmountCell
import io.horizontalsystems.bankwallet.modules.send.MemoCell
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

@Composable
fun SendTronConfirmationScreen(
    navController: NavController,
    sendViewModel: SendTronViewModel,
    amountInputModeViewModel: AmountInputModeViewModel,
    sendEntryPointDestId: Int
) {
    val closeUntilDestId = if (sendEntryPointDestId == 0) {
        R.id.sendXFragment
    } else {
        sendEntryPointDestId
    }
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
            HudHelper.showErrorMessage(view, sendResult.caution.getString())
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
        //additional close for cases when user closes app immediately after sending
        if (sendResult is SendResult.Sent) {
            navController.popBackStack(closeUntilDestId, true)
        }
    }

    HSScaffold(
        title = stringResource(R.string.Send_Confirmation_Title),
        onBack = navController::popBackStack,
    ) {
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
                            navController = navController,
                            onCopy = {
                                stat(
                                    page = StatPage.SendConfirmation,
                                    event = StatEvent.Copy(StatEntity.Address),
                                    section = StatSection.AddressTo
                                )
                            },
                            onAddToExisting = {
                                stat(
                                    page = StatPage.SendConfirmation,
                                    event = StatEvent.Open(StatPage.ContactAddToExisting),
                                    section = StatSection.AddressTo
                                )
                            },
                            onAddToNew = {
                                stat(
                                    page = StatPage.SendConfirmation,
                                    event = StatEvent.Open(StatPage.ContactNew),
                                    section = StatSection.AddressTo
                                )
                            }
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
                        HSFeeRawWithViewState(
                            title = stringResource(R.string.FeeInfo_TronFee_Title),
                            info = stringResource(R.string.FeeInfo_TronFee_Description),
                            coinCode = feeCoin.code,
                            coinDecimal = feeCoinMaxAllowedDecimals,
                            fee = fee,
                            viewState = feeViewState,
                            amountInputType = amountInputType,
                            rate = feeCoinRate,
                            navController = navController
                        )
                    }

                    activationFee?.let {
                        add {
                            HSFeeRaw(
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
                onClickSend = {
                    sendViewModel.onClickSend()

                    stat(page = StatPage.SendConfirmation, event = StatEvent.Send)
                },
                enabled = sendEnabled
            )
        }
    }
}

@Composable
private fun SendButton(
    modifier: Modifier,
    sendResult: SendResult?,
    onClickSend: () -> Unit,
    enabled: Boolean
) {
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
                    R.id.feeSettingsInfoDialog, FeeSettingsInfoDialog.Input(title, info)
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
