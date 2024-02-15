package cash.p.terminal.modules.walletconnect.request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.google.gson.Gson
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCRequestModule
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCSendEthereumTransactionRequestViewModel
import cash.p.terminal.modules.walletconnect.request.ui.TitleTypedValueCell
import cash.p.terminal.modules.walletconnect.session.ui.BlockchainCell
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCEthereumTransaction
import cash.p.terminal.modules.walletconnect.request.sendtransaction.WCSendEthRequestScreen
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.MessageToSign
import cash.p.terminal.ui.compose.components.ScreenMessageWithAction
import cash.p.terminal.ui.compose.components.VSpacer
import cash.p.terminal.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

class WCRequestFragment : BaseComposeFragment() {
    private val logger = AppLogger("wallet-connect request")

    @Composable
    override fun GetContent(navController: NavController) {
        val wcRequestViewModel =
            viewModel<WCNewRequestViewModel>(factory = WCNewRequestViewModel.Factory())
        val composableScope = rememberCoroutineScope()
        when (val sessionRequestUI = wcRequestViewModel.sessionRequest) {
            is SessionRequestUI.Content -> {
                if (sessionRequestUI.method == "eth_sendTransaction") {
                    val evmKitWrapper = wcRequestViewModel.evmKitWrapper ?: return
                    val transaction =
                        try {
                            val ethTransaction = Gson().fromJson(
                                sessionRequestUI.param,
                                WCEthereumTransaction::class.java
                            )
                            ethTransaction.getWCTransaction()
                        } catch (e: Throwable) {
                            return
                        }
                    val vmFactory by lazy {
                        WCRequestModule.FactoryV2(
                            evmKitWrapper,
                            transaction,
                            sessionRequestUI.peerUI.peerName
                        )
                    }
                    val viewModel by viewModels<WCSendEthereumTransactionRequestViewModel> { vmFactory }
                    val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(
                        R.id.wcRequestFragment
                    ) { vmFactory }
                    val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(R.id.wcRequestFragment) { vmFactory }
                    val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(R.id.wcRequestFragment) { vmFactory }

                    val cachedNonceViewModel = nonceViewModel //needed in SendEvmSettingsFragment

                    sendEvmTransactionViewModel.sendSuccessLiveData.observe(viewLifecycleOwner) { transactionHash ->
                        viewModel.approve(transactionHash)
                        HudHelper.showSuccessMessage(
                            requireActivity().findViewById(android.R.id.content),
                            R.string.Hud_Text_Done
                        )
                        navController.popBackStack()
                    }

                    sendEvmTransactionViewModel.sendFailedLiveData.observe(viewLifecycleOwner) {
                        HudHelper.showErrorMessage(
                            requireActivity().findViewById(android.R.id.content),
                            it
                        )
                    }

                    WCSendEthRequestScreen(
                        navController,
                        viewModel,
                        sendEvmTransactionViewModel,
                        feeViewModel,
                        logger,
                        R.id.wcRequestFragment
                    ) { navController.popBackStack() }
                } else {
                    WCNewSignRequestScreen(
                        sessionRequestUI,
                        navController,
                        onAllow = {
                            composableScope.launch {
                                try {
                                    wcRequestViewModel.allow()
                                    navController.popBackStack()
                                } catch (e: Throwable) {
                                    showError(e)
                                }
                            }
                            logger.info("allow request")
                        },
                        onDecline = {
                            composableScope.launch {
                                try {
                                    wcRequestViewModel.reject()
                                    navController.popBackStack()
                                } catch (e: Throwable) {
                                    showError(e)
                                }
                            }
                            logger.info("decline request")
                        }
                    )
                }
            }

            is SessionRequestUI.Initial -> {
                ScreenMessageWithAction(
                    text = stringResource(R.string.Error),
                    icon = R.drawable.ic_error_48
                ) {
                    ButtonPrimaryYellow(
                        modifier = Modifier
                            .padding(horizontal = 48.dp)
                            .fillMaxWidth(),
                        title = stringResource(R.string.Button_Close),
                        onClick = { navController.popBackStack() }
                    )
                }
            }
        }

    }

    private fun showError(e: Throwable) {
        HudHelper.showErrorMessage(
            requireActivity().findViewById(android.R.id.content),
            e.message ?: e::class.java.simpleName
        )
    }

}

@Composable
fun WCNewSignRequestScreen(
    sessionRequestUI: SessionRequestUI.Content,
    navController: NavController,
    onAllow: () -> Unit,
    onDecline: () -> Unit
) {
    Column(
        modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)
    ) {
        AppBar(
            stringResource(R.string.WalletConnect_SignMessageRequest_Title),
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
            VSpacer(12.dp)

            MessageContent(
                sessionRequestUI.param,
                sessionRequestUI.peerUI.peerName,
                sessionRequestUI.chainData,
            )

            VSpacer(24.dp)
        }

        ActionButtons(
            onDecline = onDecline,
            onAllow = onAllow
        )

    }

}

@Composable
private fun ActionButtons(
    onDecline: () -> Unit = {},
    onAllow: () -> Unit = {}
) {
    ButtonsGroupWithShade {
        Column(Modifier.padding(horizontal = 24.dp)) {
            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.WalletConnect_SignMessageRequest_ButtonSign),
                onClick = onAllow,
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Reject),
                onClick = onDecline
            )
        }
    }
}

@Composable
private fun MessageContent(
    message: String,
    dAppName: String?,
    wcChainData: WCChainData?,
) {
    SectionUniversalLawrence {
        dAppName?.let { dApp ->
            TitleTypedValueCell(
                stringResource(R.string.WalletConnect_SignMessageRequest_dApp),
                dApp
            )
        }
        wcChainData?.let {
            BlockchainCell(wcChainData.chain.name, wcChainData.address)
        }
    }

    MessageToSign(message)

}

