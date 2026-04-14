package com.quantum.wallet.bankwallet.modules.moneronetwork.addnode

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.Caution
import com.quantum.wallet.bankwallet.entities.DataState
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInput
import com.quantum.wallet.bankwallet.ui.compose.components.FormsInputStateWarning
import com.quantum.wallet.bankwallet.ui.compose.components.HeaderText
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold

@Composable
fun AddMoneroNodeScreen(
    navController: NavController
) {
    val viewModel = viewModel<AddMoneroNodeViewModel>(factory = AddMoneroNodeModule.Factory())
    if (viewModel.viewState.closeScreen) {
        navController.popBackStack()
        viewModel.onScreenClose()
    }

    HSScaffold(
        title = stringResource(R.string.AddMoneroNode_AddNode),
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

                Spacer(modifier = Modifier.height(12.dp))

                HeaderText(stringResource(id = R.string.AddMoneroNode_NodeUrl))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    qrScannerEnabled = true,
                    onValueChange = viewModel::onEnterRpcUrl,
                    hint = "",
                    state = getState(viewModel.viewState.urlCaution)
                )
                Spacer(modifier = Modifier.height(24.dp))

                HeaderText(stringResource(id = R.string.AddMoneroNode_Username))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onValueChange = viewModel::onEnterUsername,
                    hint = ""
                )

                Spacer(modifier = Modifier.height(24.dp))

                HeaderText(stringResource(id = R.string.AddMoneroNode_Pasword))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onValueChange = viewModel::onEnterPassword,
                    hint = ""
                )
                Spacer(Modifier.height(60.dp))
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
