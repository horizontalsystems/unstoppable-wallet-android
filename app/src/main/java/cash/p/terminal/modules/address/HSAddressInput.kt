package cash.p.terminal.modules.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.entities.Address
import cash.p.terminal.ui.compose.components.FormsInputAddress
import cash.p.terminal.ui.compose.components.TextPreprocessor
import cash.p.terminal.ui.compose.components.TextPreprocessorImpl
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
) {
    val viewModel = viewModel<AddressViewModel>(
        factory = AddressInputModule.FactoryToken(tokenQuery, coinCode, initial),
        key = "address_view_model_${tokenQuery.id}"
    )

    HSAddressInput(
        modifier = modifier,
        viewModel = viewModel,
        error = error,
        textPreprocessor = textPreprocessor,
        navController = navController,
        onError = onError,
        onValueChange = onValueChange
    )
}

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    navController: NavController,
    onError: ((Throwable?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null,
) {
    LaunchedEffect(error) {
        viewModel.onAddressError(error)
    }

    LaunchedEffect(viewModel.address) {
        onValueChange?.invoke(viewModel.address)
    }

    LaunchedEffect(viewModel.inputState) {
        onError?.invoke(viewModel.inputState?.errorOrNull)
    }

    FormsInputAddress(
        modifier = modifier,
        value = viewModel.value,
        hint = stringResource(id = R.string.Send_Hint_Address),
        state = viewModel.inputState,
        textPreprocessor = textPreprocessor,
        navController = navController,
        chooseContactEnable = viewModel.hasContacts(),
        blockchainType = viewModel.blockchainType,
    ) {
        viewModel.parseText(it)
    }
}