package io.horizontalsystems.bankwallet.modules.importcexaccount

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun ImportCexAccountEnterCexDataScreen(
    cexId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
) {
    val context = LocalContext.current
    var scannedData by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val viewModel =
        viewModel<ImportCexAccountEnterCexDataViewModel>(factory = ImportCexAccountEnterCexDataViewModel.Factory(cexId))

    val cex = viewModel.cex

    val menuItems = buildList {
        if (cex is CexBinance) {
            val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
                    scannedData = Pair(data, System.currentTimeMillis())
                }
            }
            add(
                MenuItem(
                    title = TranslatableString.ResString(R.string.Button_ScanQr),
                    icon = R.drawable.ic_qr_scan_24px,
                    onClick = {
                        qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
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
                    EnterCexDataBinanceForm(paddingValues, onAccountCreate, scannedData)
                }

                is CexCoinzix -> {
                    EnterCexDataCoinzixForm(paddingValues, onAccountCreate)
                }
            }
        }
    }
}

@Composable
private fun EnterCexDataCoinzixForm(paddingValues: PaddingValues, onAccountCreate: () -> Unit) {
    val viewModel = viewModel<EnterCexDataCoinzixViewModel>()

    val intent = Intent(LocalContext.current, HCaptchaActivity::class.java)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val token = data?.extras?.getString("captcha")?: ""
                    viewModel.onResultCaptchaToken(token)
                }
                Activity.RESULT_CANCELED -> {
                    Log.d("hCaptcha", "hCaptcha failed")
                }
            }
        }

    if (viewModel.accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    var hidePassphrase by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(paddingValues)) {
        InfoText(text = stringResource(R.string.ImportCexAccountConzix_Description))
        VSpacer(height = 20.dp)
        FormsInput(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.ImportCexAccountConzix_Email)
        ) {
            viewModel.onEnterEmail(it)
        }
        VSpacer(height = 16.dp)
        FormsInputPassword(
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.Password),
            //state = uiState.passphraseState,
            onValueChange = {
                viewModel.onEnterPassword(it)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            hide = hidePassphrase,
            onToggleHide = {
                hidePassphrase = !hidePassphrase
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        ButtonsGroupWithShade {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ButtonPrimaryYellowWithSpinner(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_Login),
                    showSpinner = false,
                    enabled = viewModel.loginEnabled,
                    onClick = {
                        launcher.launch(intent)
                    },
                )
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryTransparent(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.Button_SignUp),
                    onClick = {
                        //viewModel.onSignUp()
                    }
                )
            }
        }
    }
}

@Composable
private fun EnterCexDataBinanceForm(
    paddingValues: PaddingValues,
    onAccountCreate: () -> Unit,
    scannedData: Pair<String, Long>?
) {
    val viewModel = viewModel<EnterCexDataBinanceViewModel>()
    val view = LocalView.current
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val apiKey = uiState.apiKey
    val secretKey = uiState.secretKey
    val errorMessage = uiState.errorMessage
    val connectEnabled = uiState.connectEnabled

    if (accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    LaunchedEffect(scannedData) {
        scannedData?.let { (text, _) ->
            viewModel.onScannedData(text)
        }
    }

    LaunchedEffect(errorMessage, uiState) {
        errorMessage?.let {
            HudHelper.showErrorMessage(view, it)
        }
    }

    Column(modifier = Modifier.padding(paddingValues)) {
        InfoText(text = stringResource(R.string.ImportCexAccountBinance_Description))
        FormsInput(
            initial = apiKey,
            modifier = Modifier.padding(horizontal = 16.dp),
            hint = stringResource(R.string.ImportCexAccountBinance_ApiKey)
        ) {
            viewModel.onEnterApiKey(it)
        }
        VSpacer(height = 16.dp)
        FormsInput(
            initial = secretKey,
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
                    enabled = connectEnabled,
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
