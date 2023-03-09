package cash.p.terminal.modules.walletconnect.request.sendtransaction.ui

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
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.core.shorten
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.evmfee.Cautions
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.fee.FeeCell
import cash.p.terminal.modules.send.evm.settings.SendEvmSettingsFragment
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.sendevmtransaction.ViewItem
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import cash.p.terminal.modules.walletconnect.request.ui.*
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

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
    val transactionInfoItems by sendEvmTransactionViewModel.viewItemsLiveData.observeAsState()
    val approveEnabled by sendEvmTransactionViewModel.sendEnabledLiveData.observeAsState(false)
    val cautions by sendEvmTransactionViewModel.cautionsLiveData.observeAsState()
    val fee by feeViewModel.feeLiveData.observeAsState(null)
    val viewState by feeViewModel.viewStateLiveData.observeAsState()

    ComposeAppTheme {
        Column(
            modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
        ) {
            AppBar(
                title = TranslatableString.ResString(R.string.WalletConnect_UnknownRequest_Title),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.SendEvmSettings_Title),
                        icon = R.drawable.ic_manage_2,
                        tint = ComposeAppTheme.colors.jacob,
                        onClick = {
                            navController.slideFromBottom(
                                resId = R.id.sendEvmSettingsFragment,
                                args = SendEvmSettingsFragment.prepareParams(parentNavGraphId)
                            )
                        }
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
                        CellUniversalLawrenceSection(section.viewItems) { item ->
                            when (item) {
                                is ViewItem.Subhead -> SubheadCell(
                                    item.title,
                                    item.value,
                                    item.iconRes
                                )
                                is ViewItem.Value -> TitleTypedValueCell(
                                    item.title,
                                    item.value,
                                    item.type
                                )
                                is ViewItem.Address -> TransactionInfoAddressCell(
                                    item.title,
                                    item.value
                                )
                                is ViewItem.ContactItem -> TransactionInfoContactCell(
                                    item.contact.name
                                )
                                is ViewItem.Input -> TitleHexValueCell(
                                    Translator.getString(R.string.WalletConnect_Input),
                                    item.value.shorten(),
                                    item.value
                                )
                                is ViewItem.Amount -> AmountCell(
                                    item.fiatAmount,
                                    item.coinAmount,
                                    item.type,
                                    item.token
                                )
                                is ViewItem.TokenItem -> TokenCell(item.token)
                                is ViewItem.AmountMulti -> AmountMultiCell(
                                    item.amounts,
                                    item.type,
                                    item.token
                                )
                                is ViewItem.NftAmount,
                                is ViewItem.ValueMulti -> {
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                CellUniversalLawrenceSection(
                    listOf {
                        FeeCell(
                            title = stringResource(R.string.FeeSettings_Fee),
                            info = stringResource(R.string.FeeSettings_Fee_Info),
                            value = fee,
                            viewState = viewState,
                            navController = navController
                        )
                    }
                )

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
