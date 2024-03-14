package io.horizontalsystems.bankwallet.modules.transactionInfo.resendbitcoin

import android.os.Parcelable
import androidx.compose.foundation.background
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
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInputX
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.modules.amount.AmountInputType
import io.horizontalsystems.bankwallet.modules.evmfee.EvmSettingsInput
import io.horizontalsystems.bankwallet.modules.fee.HSFeeRaw
import io.horizontalsystems.bankwallet.modules.hodler.HSHodler
import io.horizontalsystems.bankwallet.modules.send.ConfirmAmountCell
import io.horizontalsystems.bankwallet.modules.send.SendButton
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionInfoOptionsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.SectionTitleCell
import io.horizontalsystems.bankwallet.ui.compose.components.TitleAndValueCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoAddressCell
import io.horizontalsystems.bankwallet.ui.compose.components.TransactionInfoContactCell
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize

class ResendBitcoinFragment : BaseComposeFragment() {

    @Parcelize
    data class Input(val optionType: TransactionInfoOptionsModule.Type) : Parcelable

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
                HudHelper.showErrorMessage(view, uiState.sendResult.caution.getString())
            }

            null -> Unit
        }

        uiState.feeCaution?.let {
            HudHelper.showErrorMessage(view, it.getString())
        }

        LaunchedEffect(uiState.sendResult) {
            if (uiState.sendResult == SendResult.Sent) {
                delay(1200)
                navController.popBackStack(closeUntilDestId, true)
            }
        }

        DisposableLifecycleCallbacks(
            //additional close for cases when user closes app immediately after sending
            onResume = {
                if (uiState.sendResult == SendResult.Sent) {
                    navController.popBackStack(closeUntilDestId, true)
                }
            }
        )

        Column(Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(uiState.titleResId),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf()
            )

            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 106.dp)
            ) {
                InfoText(text = stringResource(uiState.descriptionResId))
                Spacer(modifier = Modifier.height(12.dp))

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

                        ConfirmAmountCell(currencyAmount, coinAmount, uiState.coin.imageUrl)
                    }
                    add {
                        TransactionInfoAddressCell(
                            title = stringResource(uiState.addressTitleResId),
                            value = uiState.address.hex,
                            showAdd = uiState.contact == null,
                            blockchainType = uiState.blockchainType,
                            navController = navController
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

                Spacer(modifier = Modifier.height(28.dp))

                val bottomSectionItems = buildList<@Composable () -> Unit> {
                    add {
                        HSFeeRaw(
                            coinCode = uiState.feeCoin.code,
                            coinDecimal = uiState.coinMaxAllowedDecimals,
                            fee = uiState.fee,
                            amountInputType = AmountInputType.COIN,
                            rate = uiState.coinRate,
                            navController = navController
                        )
                    }
                }

                CellUniversalLawrenceSection(bottomSectionItems)

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
            }

            Spacer(modifier = Modifier.weight(1f))
            SendButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
                sendResult = uiState.sendResult,
                onClickSend = resendViewModel::onClickSend
            )
        }
    }
}
