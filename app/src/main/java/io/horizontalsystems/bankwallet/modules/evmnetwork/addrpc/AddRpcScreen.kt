package io.horizontalsystems.bankwallet.modules.evmnetwork.addrpc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Caution
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Blockchain

@Composable
fun AddRpcScreen(
    navController: NavController,
    blockchain: Blockchain,
) {
    val viewModel = viewModel<AddRpcViewModel>(factory = AddRpcModule.Factory(blockchain))
    if (viewModel.viewState.closeScreen) {
        navController.popBackStack()
        viewModel.onScreenClose()
    }

    HSScaffold(
        title = stringResource(R.string.AddEvmSyncSource_AddRPCSource),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = {
                    navController.popBackStack()
                }
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)

                HeaderText(stringResource(id = R.string.AddEvmSyncSource_RpcUrl))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    qrScannerEnabled = true,
                    onValueChange = viewModel::onEnterRpcUrl,
                    hint = "",
                    state = getState(viewModel.viewState.urlCaution)
                )
                VSpacer(24.dp)

                HeaderText(stringResource(id = R.string.AddEvmSyncSource_BasicAuthentication))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    qrScannerEnabled = true,
                    onValueChange = viewModel::onEnterBasicAuth,
                    hint = ""
                )
                VSpacer(60.dp)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_Add),
                    onClick = { viewModel.onAddClick() },
                )
            }
        }
    }
}

private fun getState(caution: Caution?) = when (caution?.type) {
    Caution.Type.Error -> DataState.Error(Exception(caution.text))
    Caution.Type.Warning -> DataState.Error(FormsInputStateWarning(caution.text))
    null -> null
}