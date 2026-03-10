package io.horizontalsystems.bankwallet.modules.transactionInfo.resendbitcoin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheetScreen
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.fee.HSFee
import io.horizontalsystems.bankwallet.modules.hodler.HSHodler
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.send.ConfirmAmountCell
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoScreen
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.components.TitleAndValueCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class ResendBitcoinScreen(val optionType: SpeedUpCancelType) : HSScreen(
    parentScreenClass = TransactionInfoScreen::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val transactionInfoViewModel = viewModel<TransactionInfoViewModel>()

        val vmFactory = remember {
            ResendBitcoinModule.Factory(
                optionType,
                transactionInfoViewModel.transactionRecord as BitcoinOutgoingTransactionRecord,
                transactionInfoViewModel.source
            )
        }

        val resendViewModel = viewModel<ResendBitcoinViewModel>(factory = vmFactory)

        ResendBitcoinScreen(
            backStack = backStack,
            resendViewModel = resendViewModel
        )
    }
}

class ResendBitcoinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }
}

@Composable
fun ResendBitcoinScreen(
    backStack: NavBackStack<HSScreen>,
    resendViewModel: ResendBitcoinViewModel
) {
    val closeUntilDestId = TransactionInfoScreen::class
    val uiState = resendViewModel.uiState

    val view = LocalView.current
    when (uiState.sendResult) {
        is SendResult.Sent -> {
            HudHelper.showSuccessMessage(
                view,
                R.string.Send_Success,
                SnackbarDuration.LONG
            )
        }

        is SendResult.Failed -> {
            backStack.add(ErrorBottomSheetScreen(uiState.sendResult.caution.getString()))
        }

        else -> Unit
    }

    LaunchedEffect(uiState.sendResult) {
        if (uiState.sendResult is SendResult.Sent) {
            delay(1200)
//            TODO("xxx nav3")
//            backStack.popBackStack(closeUntilDestId, true)
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        //additional close for cases when user closes app immediately after sending
        if (uiState.sendResult is SendResult.Sent) {
            backStack.removeLastUntil(closeUntilDestId, true)
        }
    }

    HSScaffold(
        title = stringResource(uiState.titleResId),
        onBack = backStack::removeLastOrNull,
    ) {
        Column {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .padding(bottom = 106.dp)
            ) {
                VSpacer(height = 12.dp)
                val topSectionItems = buildList<@Composable () -> Unit> {
                    add {
                        SectionTitleCell(
                            stringResource(R.string.Send_Confirmation_YouSend),
                            uiState.coin.name,
                            R.drawable.ic_arrow_up_right_12
                        )
                    }
                    add {
                        val coinAmount = App.numberFormatter.formatCoinFull(
                            uiState.amount,
                            uiState.coin.code,
                            uiState.coinMaxAllowedDecimals
                        )

                        val currencyAmount = uiState.coinRate?.let { rate ->
                            rate.copy(value = uiState.amount.times(rate.value))
                                .getFormattedFull()
                        }

                        ConfirmAmountCell(currencyAmount, coinAmount, uiState.coin)
                    }
                    add {
                        TransactionInfoAddressCell(
                            title = stringResource(uiState.addressTitleResId),
                            value = uiState.address.hex,
                            showAdd = uiState.contact == null,
                            blockchainType = uiState.blockchainType,
                            backStack = backStack,
                            onCopy = {
                                stat(
                                    page = StatPage.Resend,
                                    event = StatEvent.Copy(StatEntity.Address),
                                    section = StatSection.AddressTo
                                )
                            },
                            onAddToExisting = {
                                stat(
                                    page = StatPage.Resend,
                                    event = StatEvent.Open(StatPage.ContactAddToExisting),
                                    section = StatSection.AddressTo
                                )
                            },
                            onAddToNew = {
                                stat(
                                    page = StatPage.Resend,
                                    event = StatEvent.Open(StatPage.ContactNew),
                                    section = StatSection.AddressTo
                                )
                            }
                        )
                    }
                    uiState.contact?.let {
                        add {
                            TransactionInfoContactCell(name = it.name)
                        }
                    }
                    if (uiState.lockTimeInterval != null) {
                        add {
                            HSHodler(lockTimeInterval = uiState.lockTimeInterval)
                        }
                    }

                    add {
                        TitleAndValueCell(
                            title = stringResource(R.string.TransactionInfoOptions_Rbf_ReplacedTransactions),
                            value = uiState.replacedTransactionHashes.size.toString()
                        )
                    }

                }

                CellUniversalLawrenceSection(topSectionItems)

                VSpacer(16.dp)
                HSFee(
                    coinCode = uiState.feeCoin.code,
                    coinDecimal = uiState.coinMaxAllowedDecimals,
                    fee = uiState.fee,
                    amountInputType = AmountInputType.COIN,
                    rate = uiState.coinRate,
                    backStack = backStack
                )

                Spacer(modifier = Modifier.height(24.dp))
                EvmSettingsInput(
                    title = stringResource(R.string.TransactionInfoOptions_Rbf_FeeTitle),
                    info = stringResource(R.string.FeeSettings_FeeRate_Info),
                    value = uiState.minFee.toBigDecimal(),
                    decimals = 0,
                    caution = uiState.feeCaution,
                    backStack = backStack,
                    onValueChange = {
                        resendViewModel.setMinFee(it.toLong())
                    },
                    onClickIncrement = {
                        resendViewModel.incrementMinFee()
                    },
                    onClickDecrement = {
                        resendViewModel.decrementMinFee()
                    }
                )

                uiState.feeCaution?.let {
                    FeeRateCaution(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                        feeRateCaution = it
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            ResendButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                titleResId = uiState.sendButtonTitleResId,
                error = uiState.feeCaution?.type == HSCaution.Type.Error,
                sendResult = uiState.sendResult,
                onClickSend = resendViewModel::onClickSend
            )
        }
    }
}

@Composable
private fun ResendButton(
    modifier: Modifier,
    titleResId: Int,
    error: Boolean,
    sendResult: SendResult?,
    onClickSend: () -> Unit
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
                title = stringResource(titleResId),
                onClick = onClickSend,
                enabled = !error
            )
        }
    }
}
