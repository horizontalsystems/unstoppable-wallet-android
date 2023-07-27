package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessorImpl
import io.horizontalsystems.marketkit.models.TokenQuery

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
        hint = stringResource(id = R.string.Watch_Address_Hint),
        state = viewModel.inputState,
        textPreprocessor = textPreprocessor,
        navController = navController,
        chooseContactEnable = viewModel.hasContacts(),
        blockchainType = viewModel.blockchainType,
    ) {
        viewModel.parseAddress(it)
    }
}