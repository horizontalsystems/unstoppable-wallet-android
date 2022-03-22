package io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCell
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeSettingsFragment
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.AmountCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.SubheadCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleHexValueCell
import io.horizontalsystems.bankwallet.modules.walletconnect.request.ui.TitleTypedValueCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*

@Composable
fun SendEthRequestScreen(
    navController: NavController,
    viewModel: WCSendEthereumTransactionRequestViewModel,
    sendEvmTransactionViewModel: SendEvmTransactionViewModel,
    feeViewModel: EvmFeeCellViewModel,
    logger: AppLogger,
    parentNavGraphId: Int,
    close: () -> Unit,
) {

    val title by sendEvmTransactionViewModel.transactionTitleLiveData.observeAsState("")
    val transactionInfoItems by sendEvmTransactionViewModel.viewItemsLiveData.observeAsState()
    val approveEnabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)
    val cautions by sendEvmTransactionViewModel.cautionsLiveData.observeAsState()
    val fee by feeViewModel.feeLiveData.observeAsState("")
    val viewState by feeViewModel.viewStateLiveData.observeAsState()
    val loading by feeViewModel.loadingLiveData.observeAsState(false)

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.PlainString(title),
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = { navController.popBackStack() }
                    )
                )
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Spacer(Modifier.height(12.dp))
                transactionInfoItems?.let { sections ->
                    sections.forEach { section ->
                        CellSingleLineLawrenceSection(section.viewItems) { item ->
                            when (item) {
                                is ViewItem.Subhead -> SubheadCell(item.title, item.value)
                                is ViewItem.Value -> TitleTypedValueCell(
                                    item.title,
                                    item.value,
                                    item.type
                                )
                                is ViewItem.Address -> TitleHexValueCell(
                                    item.title,
                                    item.valueTitle,
                                    item.value
                                )
                                is ViewItem.Input -> TitleHexValueCell(
                                    Translator.getString(R.string.WalletConnect_Input),
                                    item.value,
                                    item.value
                                )
                                is ViewItem.Amount -> AmountCell(
                                    item.fiatAmount,
                                    item.coinAmount,
                                    item.type
                                )
                                is ViewItem.Warning -> TextImportantWarning(
                                    text = item.description,
                                    title = item.title,
                                    icon = item.icon
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                EvmFeeCell(
                    title = stringResource(R.string.FeeSettings_MaxFee),
                    value = fee,
                    loading = loading,
                    viewState = viewState,
                    highlightEditButton = feeViewModel.highlightEditButton,
                ) {
                    navController.slideFromBottom(
                        resId = R.id.sendEvmFeeSettingsFragment,
                        args = EvmFeeSettingsFragment.prepareParams(parentNavGraphId)
                    )
                }

                cautions?.let {
                    Cautions(it)
                }

                Spacer(Modifier.height(24.dp))
            }
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Confirm),
                    enabled = approveEnabled,
                    onClick = {
                        logger.info("click confirm button")
                        sendEvmTransactionViewModel.send(logger)
                    }
                )
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Reject),
                    onClick = {
                        viewModel.reject()
                        close()
                    }
                )
                Spacer(Modifier.height(32.dp))
            }

        }
    }
}
