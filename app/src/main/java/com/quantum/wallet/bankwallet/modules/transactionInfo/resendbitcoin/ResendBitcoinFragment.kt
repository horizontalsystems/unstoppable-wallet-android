package com.quantum.wallet.bankwallet.modules.transactionInfo.resendbitcoin

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.HSCaution
import com.quantum.wallet.bankwallet.core.getInputX
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.StatSection
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.modules.amount.AmountInputType
import com.quantum.wallet.bankwallet.modules.confirm.ErrorBottomSheet
import com.quantum.wallet.bankwallet.modules.evmfee.EvmSettingsInput
import com.quantum.wallet.bankwallet.modules.fee.HSFee
import com.quantum.wallet.bankwallet.modules.hodler.HSHodler
import com.quantum.wallet.bankwallet.modules.send.ConfirmAmountCell
import com.quantum.wallet.bankwallet.modules.send.SendResult
import com.quantum.wallet.bankwallet.modules.send.bitcoin.advanced.FeeRateCaution
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionInfoViewModel
import com.quantum.wallet.bankwallet.modules.transactionInfo.options.SpeedUpCancelType
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import com.quantum.wallet.bankwallet.ui.compose.components.SectionTitleCell
import com.quantum.wallet.bankwallet.ui.compose.components.TitleAndValueCell
import com.quantum.wallet.bankwallet.ui.compose.components.TransactionInfoAddressCell
import com.quantum.wallet.bankwallet.ui.compose.components.TransactionInfoContactCell
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.SnackbarDuration
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

class ResendBitcoinFragment : BaseComposeFragment() {

    @Parcelize
    data class Input(val optionType: SpeedUpCancelType) : Parcelable

    private val transactionInfoViewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment)

    private val input by lazy {
        requireArguments().getInputX<Input>()!!
    }

    private val vmFactory by lazy {
        ResendBitcoinModule.Factory(
            input.optionType,
            transactionInfoViewModel.transactionRecord as BitcoinOutgoingTransactionRecord,
            transactionInfoViewModel.source
        )
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val resendViewModel by viewModels<ResendBitcoinViewModel> { vmFactory }

        ResendBitcoinScreen(
            navController = navController,
            resendViewModel = resendViewModel
        )
    }

    @Composable
    private fun ResendBitcoinScreen(
        navController: NavController,
        resendViewModel: ResendBitcoinViewModel
    ) {
        val closeUntilDestId = R.id.transactionInfoFragment
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
                navController.slideFromBottom(R.id.errorBottomSheet, ErrorBottomSheet.Input(uiState.sendResult.caution.getString()))
            }

            else -> Unit
        }

        LaunchedEffect(uiState.sendResult) {
            if (uiState.sendResult is SendResult.Sent) {
                delay(1200)
                navController.popBackStack(closeUntilDestId, true)
            }
        }

        LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
            //additional close for cases when user closes app immediately after sending
            if (uiState.sendResult is SendResult.Sent) {
                navController.popBackStack(closeUntilDestId, true)
            }
        }

        HSScaffold(
            title = stringResource(uiState.titleResId),
            onBack = navController::popBackStack,
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
                                navController = navController,
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
                        navController = navController
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    EvmSettingsInput(
                        title = stringResource(R.string.TransactionInfoOptions_Rbf_FeeTitle),
                        info = stringResource(R.string.FeeSettings_FeeRate_Info),
                        value = uiState.minFee.toBigDecimal(),
                        decimals = 0,
                        caution = uiState.feeCaution,
                        navController = navController,
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
