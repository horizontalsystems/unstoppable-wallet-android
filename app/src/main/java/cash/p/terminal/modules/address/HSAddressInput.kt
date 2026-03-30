package cash.p.terminal.modules.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.navigation.openQrScanner
import cash.p.terminal.entities.Address
import cash.p.terminal.ui_compose.entities.DataState
import cash.p.terminal.ui.compose.components.FormsInputAddress
import cash.p.terminal.ui.compose.components.TextPreprocessor
import cash.p.terminal.ui.compose.components.TextPreprocessorImpl
import cash.p.terminal.wallet.title
import cash.p.terminal.wallet.entities.TokenQuery

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    initial: Address? = null,
    tokenQuery: TokenQuery,
    coinCode: String,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    navController: NavController,
    onError: ((Throwable?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null,
    isPoisonAddress: Boolean = false,
) {
    val viewModel = viewModel<AddressViewModel>(
        factory = AddressInputModule.FactoryToken(tokenQuery, coinCode, initial),
        key = "address_view_model_${tokenQuery.id}"
    )

    HSAddressInput(
        modifier = modifier,
        viewModel = viewModel,
        inputState = viewModel.inputState.collectAsStateWithLifecycle().value,
        address = viewModel.address.collectAsStateWithLifecycle().value,
        value = viewModel.value.collectAsStateWithLifecycle().value,
        error = error,
        textPreprocessor = textPreprocessor,
        navController = navController,
        onError = onError,
        onValueChange = onValueChange,
        isPoisonAddress = isPoisonAddress,
    )
}

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel,
    inputState: DataState<Address>?,
    address: Address?,
    value: String,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    navController: NavController,
    onError: ((Throwable?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null,
    isPoisonAddress: Boolean = false,
) {
    LaunchedEffect(error) {
        viewModel.onAddressError(error)
    }

    LaunchedEffect(address) {
        onValueChange?.invoke(address)
    }

    LaunchedEffect(inputState) {
        onError?.invoke(inputState?.errorOrNull)
    }

    val scannerTitle = stringResource(R.string.qr_scanner_title_address, viewModel.blockchainType.title)

    FormsInputAddress(
        modifier = modifier,
        value = value,
        hint = stringResource(id = R.string.Send_Hint_Address),
        state = inputState,
        textPreprocessor = textPreprocessor,
        navController = navController,
        chooseContactEnable = viewModel.hasContacts(),
        blockchainType = viewModel.blockchainType,
        onQrScanClick = {
            navController.openQrScanner(scannerTitle) { scannedText ->
                val textProcessed = textPreprocessor.process(scannedText)
                viewModel.parseText(textProcessed)
            }
        },
        onValueChange = {
            viewModel.parseText(it)
        },
        isPoisonAddress = isPoisonAddress,
    )
}
