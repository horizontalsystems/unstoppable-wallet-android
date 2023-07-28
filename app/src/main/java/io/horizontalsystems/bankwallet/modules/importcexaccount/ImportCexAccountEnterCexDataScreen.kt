package io.horizontalsystems.bankwallet.modules.importcexaccount

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationMode
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationViewModel
import io.horizontalsystems.bankwallet.modules.coinzixverify.TwoFactorType
import io.horizontalsystems.bankwallet.modules.coinzixverify.ui.CoinzixVerificationScreen
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerActivity
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithSpinner
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputPassword
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@Composable
fun ImportCexAccountEnterCexDataScreen(
    cexId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
    onShowError: (title: TranslatableString, description: TranslatableString) -> Unit,
) {
    val viewModel = viewModel<ImportCexAccountEnterCexDataViewModel>(factory = ImportCexAccountEnterCexDataViewModel.Factory(cexId))

    when (val cex = viewModel.cex) {
        is CexBinance -> {
            ImportBinanceCexAccountScreen(cex, onNavigateBack, onClose, onAccountCreate)
        }

        is CexCoinzix -> {
            ImportCoinzixCexAccountNavHost(cex, onNavigateBack, onClose, onAccountCreate, onShowError)
        }

        else -> Unit
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ImportCoinzixCexAccountNavHost(
    cex: CexCoinzix,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
    onShowError: (title: TranslatableString, description: TranslatableString) -> Unit
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "import-coinzix-account",
    ) {
        composable("import-coinzix-account") {
            ImportCoinzixCexAccountScreen(
                cex = cex,
                onNavigateBack = onNavigateBack,
                onClose = onClose,
                openVerification = { login: CoinzixVerificationMode.Login ->
                    navController.navigate("login-verification/${login.token}/${login.secret}/?steps=${login.twoFactorTypes.joinToString { "${it.code}" }}")
                },
                onShowError = onShowError
            )
        }

        composablePage("login-verification/{token}/{secret}/?steps={steps}") { backStackEntry ->
            val token = backStackEntry.arguments?.getString("token") ?: return@composablePage
            val secret = backStackEntry.arguments?.getString("secret") ?: return@composablePage
            val steps: List<Int> = backStackEntry.arguments?.getString("steps")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: listOf()

            val coinzixVerificationViewModel = viewModel<CoinzixVerificationViewModel>(
                factory = CoinzixVerificationViewModel.FactoryForLogin(token, secret, steps.mapNotNull { TwoFactorType.fromCode(it) })
            )

            CoinzixVerificationScreen(
                viewModel = coinzixVerificationViewModel,
                onSuccess = onAccountCreate,
                onNavigateBack = { navController.popBackStack() },
                onClose = onClose,
                onShowError = { text ->
                    onShowError(
                        TranslatableString.ResString(R.string.ImportCexAccountConzix_Error_Title),
                        text
                    )
                }
            )
        }
    }
}

@Composable
private fun ImportCoinzixCexAccountScreen(
    cex: CexCoinzix,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onShowError: (title: TranslatableString, description: TranslatableString) -> Unit,
    openVerification: (CoinzixVerificationMode.Login) -> Unit
) {
    val viewModel = viewModel<EnterCexDataCoinzixViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var hidePassphrase by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.PlainString(cex.name),
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                InfoText(text = stringResource(R.string.ImportCexAccountConzix_Description))
                VSpacer(height = 20.dp)
                val inputsEnabled = !loading
                val textColor = if (inputsEnabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey50
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ImportCexAccountConzix_Email),
                    textColor = textColor,
                    enabled = inputsEnabled,
                    pasteEnabled = false,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                ) {
                    viewModel.onEnterEmail(it)
                }
                VSpacer(height = 16.dp)
                FormsInputPassword(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.Password),
                    textColor = textColor,
                    enabled = inputsEnabled,
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
                VSpacer(height = 32.dp)
            }
            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Login),
                        showSpinner = loading,
                        enabled = viewModel.loginEnabled && !loading,
                        onClick = {
                            coroutineScope.launch {
                                loading = true
                                try {
                                    val login = viewModel.login()
                                    openVerification.invoke(login)
                                } catch (error: Throwable) {
                                    onShowError(
                                        TranslatableString.ResString(R.string.ImportCexAccountConzix_Error_Title),
                                        TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
                                    )
                                }
                                loading = false
                            }
                        },
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_SignUp),
                        onClick = {
                            uriHandler.openUri("https://coinzix.com/sign-up")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportBinanceCexAccountScreen(
    cex: CexBinance,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit
) {
    val viewModel = viewModel<EnterCexDataBinanceViewModel>()
    val view = LocalView.current
    val context = LocalContext.current
    val uiState = viewModel.uiState
    val accountCreated = uiState.accountCreated
    val apiKey = uiState.apiKey
    val secretKey = uiState.secretKey
    val errorMessage = uiState.errorMessage
    val connectEnabled = uiState.connectEnabled
    val uriHandler = LocalUriHandler.current

    if (accountCreated) {
        LaunchedEffect(Unit) {
            onAccountCreate.invoke()
        }
    }

    LaunchedEffect(errorMessage, uiState) {
        errorMessage?.let {
            HudHelper.showErrorMessage(view, it)
        }
    }

    val qrScannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringExtra(ModuleField.SCAN_ADDRESS) ?: ""
            viewModel.onScannedData(data)
        }
    }

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.PlainString(cex.name),
                navigationIcon = {
                    HsBackButton(onClick = onNavigateBack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_ScanQr),
                        icon = R.drawable.ic_qr_scan_24px,
                        onClick = {
                            qrScannerLauncher.launch(QRScannerActivity.getScanQrIntent(context))
                        }
                    ),
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                InfoText(text = stringResource(R.string.ImportCexAccountBinance_Description))
                FormsInput(
                    initial = apiKey,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ImportCexAccountBinance_ApiKey)
                ) {
                    viewModel.onEnterApiKey(it)
                }
                VSpacer(16.dp)
                FormsInput(
                    initial = secretKey,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hint = stringResource(R.string.ImportCexAccountBinance_SecretKey)
                ) {
                    viewModel.onEnterSecretKey(it)
                }
                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellowWithSpinner(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_Connect),
                        enabled = connectEnabled,
                        showSpinner = uiState.showSpinner,
                        onClick = {
                            viewModel.onClickConnect()
                        },
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_GetApiKeys),
                        onClick = {
                            uriHandler.openUri("https://www.binance.com/en/my/settings/api-management")
                        }
                    )
                }
            }
        }
    }
}
