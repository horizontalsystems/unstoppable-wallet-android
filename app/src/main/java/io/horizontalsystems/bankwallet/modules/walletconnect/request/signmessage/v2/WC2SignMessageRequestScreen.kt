package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.ui.SignMessageRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantWarning

@Composable
fun WC2UnsupportedRequestScreen(
    navController: NavController,
    requestData: WC2SessionManager.RequestData
) {
    val viewModel = viewModel<WC2UnsupportedRequestViewModel>(factory = WC2UnsupportedRequestViewModel.Factory(requestData))

    ComposeAppTheme {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = stringResource(R.string.WalletConnect_UnsupportedRequest_Title),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Close),
                            icon = R.drawable.ic_close,
                            onClick = { navController.popBackStack() }
                        )
                    )
                )
            }
        ) {
            Column(
                modifier = Modifier.padding(it)
            ) {
                Spacer(modifier = Modifier.padding(top = 12.dp))
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.WalletConnect_UnsupportedRequest_WarningTitle),
                    text = stringResource(R.string.WalletConnect_UnsupportedRequest_WarningDescription),
                    icon = R.drawable.ic_attention_20
                )
                Spacer(modifier = Modifier.weight(1f))
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.WalletConnect_SignMessageRequest_ButtonSign),
                        enabled = false,
                        onClick = { },
                    )
                    Spacer(Modifier.height(16.dp))
                    ButtonPrimaryDefault(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Reject),
                        onClick = {
                            viewModel.reject()
                            navController.popBackStack()
                        }
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun WC2SignMessageRequestScreen(
    navController: NavController,
    requestData: WC2SessionManager.RequestData
) {
    val viewModel = viewModel<WCSignMessageRequestViewModel>(
        factory = WCSignMessageRequestModule.FactoryWC2(
            requestData
        )
    )

    val close by viewModel.closeLiveEvent.observeAsState()
    LaunchedEffect(close) {
        if (close != null) {
            navController.popBackStack()
        }
    }

    SignMessageRequestScreen(
        navController,
        viewModel,
    )
}
