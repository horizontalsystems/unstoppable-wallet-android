package io.horizontalsystems.bankwallet.modules.walletconnect.request

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.isEvm
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.TitleValue
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ValueType
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCEthereumTransaction
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WCSendEthRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signtransaction.WCSignEthereumTransactionRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.session.ui.BlockchainCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.MessageToSign
import io.horizontalsystems.bankwallet.ui.compose.components.ScreenMessageWithAction
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.launch

class WCRequestFragment : BaseComposeFragment() {
    private val logger = AppLogger("wallet-connect request")

    @Composable
    override fun GetContent(navController: NavController) {
        val wcRequestRouterViewModel =
            viewModel<WCRequestRouterViewModel>(factory = WCRequestRouterViewModel.Factory())

        val uiState = wcRequestRouterViewModel.uiState

        val blockchainType = uiState.blockchainType

        if (blockchainType == null) {
            WcRequestError(navController)
        } else if (blockchainType.isEvm) {
            WcRequestEvm(navController)
        } else if (blockchainType is BlockchainType.Stellar) {
            WcRequestPreScreen(navController)
        } else {
            WcRequestError(navController)
        }
    }

    private fun showError(e: Throwable) {
        HudHelper.showErrorMessage(
            requireActivity().findViewById(android.R.id.content),
            e.message ?: e::class.java.simpleName
        )
    }

    @Composable
    fun WcRequestEvm(navController: NavController) {
        val wcRequestEvmViewModel = viewModel<WCRequestEvmViewModel>(factory = WCRequestEvmViewModel.Factory())
        val composableScope = rememberCoroutineScope()
        when (val sessionRequestUI = wcRequestEvmViewModel.sessionRequestUi) {
            is SessionRequestUI.Content -> {
                if (sessionRequestUI.method == "eth_sendTransaction") {
                    val blockchainType = wcRequestEvmViewModel.blockchainType ?: return
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

                    WCSendEthRequestScreen(
                        navController,
                        logger,
                        blockchainType,
                        transaction,
                        sessionRequestUI.peerUI.peerName
                    )
                } else if (sessionRequestUI.method == "eth_signTransaction") {
                    val blockchainType = wcRequestEvmViewModel.blockchainType ?: return

                    val transaction = try {
                        val ethTransaction = Gson().fromJson(
                            sessionRequestUI.param,
                            WCEthereumTransaction::class.java
                        )
                        ethTransaction.getWCTransaction()
                    } catch (e: Throwable) {
                        return
                    }

                    WCSignEthereumTransactionRequestScreen(
                        navController,
                        logger,
                        blockchainType,
                        transaction,
                        sessionRequestUI.peerUI.peerName
                    )
                } else {
                    WCNewSignRequestScreen(
                        sessionRequestUI,
                        navController,
                        onAllow = {
                            composableScope.launch {
                                try {
                                    wcRequestEvmViewModel.allow()
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
                                    wcRequestEvmViewModel.reject()
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
}

@Composable
fun WcRequestError(navController: NavController) {
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
                sessionRequestUI.chainName,
                sessionRequestUI.chainAddress,
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
fun ActionButtons(
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
fun MessageContent(
    message: String,
    dAppName: String?,
    chainName: String?,
    chainAddress: String?,
) {
    SectionUniversalLawrence {
        dAppName?.let { dApp ->
            TitleValue(
                ViewItem.Value(
                    title = stringResource(R.string.WalletConnect_SignMessageRequest_dApp),
                    value = dApp,
                    type = ValueType.Regular
                )
            )
        }
        chainName?.let {
            BlockchainCell(chainName, chainAddress)
        }
    }

    MessageToSign(message)

}
