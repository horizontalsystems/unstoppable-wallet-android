package cash.p.terminal.modules.walletconnect.request.sendtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.modules.confirm.ConfirmTransactionScreen
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionView
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WCSendEthRequestScreen(
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

    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            title = stringResource(R.string.Button_Confirm),
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
                            R.id.sendEvmSettingsFragment,
                            SendEvmSettingsFragment.Input(parentNavGraphId)
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
            VSpacer(12.dp)
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

                            is ViewItem.Address -> {
                                TransactionInfoAddressCell(
                                    item.title,
                                    item.value,
                                    item.showAdd,
                                    item.blockchainType,
                                    navController,
                                    onCopy = {
                                        stat(page = StatPage.WalletConnect, section = item.statSection, event = StatEvent.Copy(StatEntity.Address))
                                    },
                                    onAddToExisting = {
                                        stat(page = StatPage.WalletConnect, section = item.statSection, event = StatEvent.Open(StatPage.ContactAddToExisting))
                                    },
                                    onAddToNew = {
                                        stat(page = StatPage.WalletConnect, section = item.statSection, event = StatEvent.Open(StatPage.ContactNew))
                                    }
                                )

                            }

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
                    VSpacer(12.dp)
                }
            }

            CellUniversalLawrenceSection(
                listOf {
                    FeeCell(
                        title = stringResource(R.string.FeeSettings_NetworkFee),
                        info = stringResource(R.string.FeeSettings_NetworkFee_Info),
                        value = fee,
                        viewState = viewState,
                        navController = navController
                    )
                }
            )

            cautions?.let {
                Cautions(it)
            }

            VSpacer(24.dp)
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Confirm),
                enabled = approveEnabled,
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(view, R.string.Send_Sending, SnackbarDuration.INDEFINITE)

                        try {
                            logger.info("click confirm button")
                            viewModel.confirm()
                            logger.info("success")

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                        } catch (t: Throwable) {
                            logger.warning("failed", t)
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                        }

                        buttonEnabled = true
                        navController.popBackStack()
                    }
                }
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = {
                    viewModel.reject()
                    close()
                }
            )
            VSpacer(32.dp)
        }
    ) {
        SendEvmTransactionView(
            navController,
            uiState.sectionViewItems,
            uiState.cautions,
            uiState.transactionFields,
            uiState.networkFee,
        )
    }
}
