package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(modifier: Modifier, coinCode: String, onValueChange: (Address?) -> Unit) {
    val viewModel = viewModel<AddressViewModel>(factory = AddressInputModule.Factory(coinCode))

    val scope = rememberCoroutineScope()
    var addressState by remember { mutableStateOf<DataState<Unit>?>(null) }
    var parseAddressJob by remember { mutableStateOf<Job?>(null)}
    var isFocused by remember { mutableStateOf(false)}

    val inputState = when {
        isFocused -> getFocusedState(addressState)
        else -> addressState
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
            var address: Address?

            addressState = DataState.Loading
            try {
                address = viewModel.parseAddress(it)
                addressState = DataState.Success(Unit)
            } catch (e: AddressValidationException) {
                address = null
                addressState = DataState.Error(e)
            }

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
