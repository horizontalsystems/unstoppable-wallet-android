package cash.p.terminal.modules.importcexaccount

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
import cash.p.terminal.R
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.utils.ModuleField
import cash.p.terminal.modules.coinzixverify.CoinzixVerificationMode
import cash.p.terminal.modules.coinzixverify.CoinzixVerificationViewModel
import cash.p.terminal.modules.coinzixverify.TwoFactorType
import cash.p.terminal.modules.coinzixverify.ui.CoinzixVerificationScreen
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.qrscanner.QRScannerActivity
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.AppBar
import cash.p.terminal.ui.compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui.compose.components.ButtonPrimaryYellowWithSpinner
import cash.p.terminal.ui.compose.components.FormsInput
import cash.p.terminal.ui.compose.components.FormsInputPassword
import cash.p.terminal.ui.compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui.compose.components.MenuItem
import cash.p.terminal.ui.compose.components.VSpacer
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.launch

@Composable
fun ImportCexAccountEnterCexDataScreen(
    cexId: String,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    onAccountCreate: () -> Unit,
) {
    val viewModel = viewModel<ImportCexAccountEnterCexDataViewModel>(factory = ImportCexAccountEnterCexDataViewModel.Factory(cexId))

    when (val cex = viewModel.cex) {
        is CexBinance -> {
            ImportBinanceCexAccountScreen(cex, onNavigateBack, onClose, onAccountCreate)
        }

        is CexCoinzix -> {
            ImportCoinzixCexAccountNavHost(cex, onNavigateBack, onClose, onAccountCreate)
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
    onAccountCreate: () -> Unit
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
                }
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
                onClose = onClose
            )
        }
    }
}

@Composable
private fun ImportCoinzixCexAccountScreen(
    cex: CexCoinzix,
    onNavigateBack: () -> Unit,
    onClose: () -> Unit,
    openVerification: (CoinzixVerificationMode.Login) -> Unit
) {
    val viewModel = viewModel<EnterCexDataCoinzixViewModel>()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val uiState = viewModel.uiState
    val error = uiState.error

    LaunchedEffect(error) {
        error?.let {
            HudHelper.showErrorMessage(view, it.message ?: it.javaClass.simpleName)
        }
    }

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
                    enabled = inputsEnabled
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
                        enabled = uiState.loginEnabled && !loading,
                        onClick = {
                            coroutineScope.launch {
                                loading = true
                                try {
                                    val login = viewModel.login()
                                    openVerification.invoke(login)
                                } catch (error: Throwable) {
                                    HudHelper.showErrorMessage(view, error.message ?: error.javaClass.simpleName)
                                }
                                loading = false
                            }
                        },
                    )
                    VSpacer(16.dp)
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Button_SignUp),
                        enabled = false,
                        onClick = {
                            //viewModel.onSignUp()
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
