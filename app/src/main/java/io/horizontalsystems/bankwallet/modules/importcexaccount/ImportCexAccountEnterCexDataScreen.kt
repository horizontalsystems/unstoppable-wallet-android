package cash.p.terminal.modules.importcexaccount

import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cash.p.terminal.R
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.*

@Composable
fun ImportCexAccountEnterCexDataScreen(
    cexId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
) {
    val viewModel =
        viewModel<ImportCexAccountEnterCexDataViewModel>(factory = ImportCexAccountEnterCexDataViewModel.Factory(cexId))

    val cex = viewModel.cex

    val menuItems = buildList {
        if (cex is CexBinance) {
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_ScanQr),
                    icon = R.drawable.ic_qr_scan_24px,
                    onClick = {

                    }
                )
            )
        }
        add(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = onClose
            )
        )
    }

    if (cex != null) {
        Scaffold(
            backgroundColor = ComposeAppTheme.colors.tyler,
            topBar = {
                AppBar(
                    title = TranslatableString.PlainString(cex.name),
                    navigationIcon = {
                        HsBackButton(onClick = onNavigateBack)
                    },
                    menuItems = menuItems
                )
            }
        ) { paddingValues ->
            when (cex) {
                is CexBinance -> {
                    EnterCexDataBinanceForm(paddingValues, onAccountCreate)
                }
                is CexCoinzix -> {

                }
            }

        }
    }

}

@Composable
private fun EnterCexDataBinanceForm(paddingValues: PaddingValues, onAccountCreate: () -> Unit) {
    val viewModel = viewModel<EnterCexDataBinanceViewModel>()

    if (viewModel.accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        InfoText(text = stringResource(R.string.ImportCexAccountBinance_Description))
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.ImportCexAccountBinance_ApiKey)
        ) {
            viewModel.onEnterApiKey(it)
        }
        VSpacer(height = 16.dp)
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.ImportCexAccountBinance_SecretKey)
        ) {
            viewModel.onEnterSecretKey(it)
        }

        Spacer(modifier = Modifier.weight(1f))

        ButtonsGroupWithShade {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellow(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Connect),
                    enabled = viewModel.connectEnabled,
                    onClick = {
                        viewModel.onClickConnect()
                    },
                )
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryTransparent(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_GetApiKeys),
                    onClick = {

                    }
                )
            }

        }
    }
}
