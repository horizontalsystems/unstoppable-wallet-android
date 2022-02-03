package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    coinType: CoinType,
    coinCode: String,
    error: Throwable? = null,
    onValueChange: (Address?) -> Unit
) {
    val viewModel = viewModel<AddressViewModel>(factory = AddressInputModule.Factory(coinType, coinCode))

    val scope = rememberCoroutineScope()
    var addressState by remember { mutableStateOf<DataState<Unit>?>(null) }
    var parseAddressJob by remember { mutableStateOf<Job?>(null)}
    var isFocused by remember { mutableStateOf(false)}

    val addressStateMergedWithError = if (addressState is DataState.Success && error != null) {
        DataState.Error(error)
    } else {
        addressState
    }

    val inputState = when {
        isFocused -> getFocusedState(addressStateMergedWithError)
        else -> addressStateMergedWithError
    }

    FormsInput(
        modifier = modifier,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        state = inputState,
        qrScannerEnabled = true,
        onChangeFocus = {
            isFocused = it
        }
    ) {
        parseAddressJob?.cancel()
        parseAddressJob = scope.launch {
            addressState = DataState.Loading

            var address: Address?
            var state: DataState<Unit>?
            try {
                address = viewModel.parseAddress(it)
                state = DataState.Success(Unit)
            } catch (e: AddressValidationException.Blank) {
                address = null
                state = null
            } catch (e: AddressValidationException) {
                address = null
                state = DataState.Error(e)
            }

            ensureActive()
            addressState = state
            onValueChange.invoke(address)
        }
    }
}

private fun getFocusedState(state: DataState<Unit>?): DataState<Unit>? {
    return if (state is DataState.Error && state.error !is AddressValidationException.Invalid) {
        null
    } else {
        state
    }
}
