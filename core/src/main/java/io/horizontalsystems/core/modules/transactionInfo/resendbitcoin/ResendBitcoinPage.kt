package io.horizontalsystems.core.modules.transactionInfo.resendbitcoin

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
import io.horizontalsystems.core.R
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.HSCaution
import io.horizontalsystems.core.core.stats.StatEntity
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.StatSection
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.modules.amount.AmountInputType
import io.horizontalsystems.core.modules.confirm.ErrorSheet
import io.horizontalsystems.core.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.core.modules.fee.HSFee
import io.horizontalsystems.core.modules.hodler.HSHodler
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.send.ConfirmAmountCell
import io.horizontalsystems.core.modules.send.SendResult
import io.horizontalsystems.core.modules.send.bitcoin.advanced.FeeRateCaution
import io.horizontalsystems.core.modules.transactionInfo.TransactionInfoPage
import io.horizontalsystems.core.modules.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.core.modules.transactionInfo.options.SpeedUpCancelType
import io.horizontalsystems.core.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.core.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.core.ui.compose.components.SectionTitleCell
import io.horizontalsystems.core.ui.compose.components.TitleAndValueCell
import io.horizontalsystems.core.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.core.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.core.ui.compose.components.VSpacer
import io.horizontalsystems.core.uiv3.components.HSScaffold
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable
data class ResendBitcoinPage(val input: Input) : HSPage() {

    @Serializable
    data class Input(val optionType: SpeedUpCancelType)

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val transactionInfoViewModel = navigation.viewModelForScreen<TransactionInfoViewModel>(TransactionInfoPage::class)

        val vmFactory = remember {
            ResendBitcoinModule.Factory(
                input.optionType,
                transactionInfoViewModel.transactionRecord as BitcoinOutgoingTransactionRecord,
                transactionInfoViewModel.source
            )
        }

        val resendViewModel = viewModel<ResendBitcoinViewModel>(factory = vmFactory)

        ResendBitcoinScreen(
            navigation = navigation,
            resendViewModel = resendViewModel
        )
    }

    @Composable
    private fun ResendBitcoinScreen(
        navigation: HSNavigation,
        resendViewModel: ResendBitcoinViewModel
    ) {
        val closeUntilDestId = TransactionInfoPage::class
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
                navigation.slideFromBottom(ErrorSheet(
                    ErrorSheet.Input(uiState.sendResult.caution.getString())
                ))
            }

            else -> Unit
        }

        LaunchedEffect(uiState.sendResult) {
            if (uiState.sendResult is SendResult.Sent) {
                delay(1200)
                navigation.removeLastUntil(closeUntilDestId, true)
            }
        }

        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            //additional close for cases when user closes app immediately after sending
            if (uiState.sendResult is SendResult.Sent) {
                navigation.removeLastUntil(closeUntilDestId, true)
            }
        }

        HSScaffold(
            title = stringResource(uiState.titleResId),
            onBack = navigation::removeLastOrNull,
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
                                navigation = navigation,
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
                        navigation = navigation
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    EvmSettingsInput(
                        title = stringResource(R.string.TransactionInfoOptions_Rbf_FeeTitle),
                        info = stringResource(R.string.FeeSettings_FeeRate_Info),
                        value = uiState.minFee.toBigDecimal(),
                        decimals = 0,
                        caution = uiState.feeCaution,
                        navigation = navigation,
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
}
