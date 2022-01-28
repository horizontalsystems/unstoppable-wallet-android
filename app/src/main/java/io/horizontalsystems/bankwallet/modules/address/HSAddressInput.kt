package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(modifier: Modifier, coinCode: String, onValueChange: (Address?) -> Unit) {
    val viewModel = viewModel<AddressViewModel>(factory = AddressInputModule.Factory(coinCode))

    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<DataState<Unit>?>(null) }
    var parseAddressJob by remember { mutableStateOf<Job?>(null)}

    FormsInput(
        modifier = modifier,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        state = state,
        qrScannerEnabled = true
    ) {
        parseAddressJob?.cancel()
        parseAddressJob = scope.launch {
            state = DataState.Loading
            val addressState = viewModel.parseAddress(it)

            state = when (addressState) {
                is DataState.Error -> DataState.Error(addressState.error.convertedError)
                DataState.Loading -> DataState.Loading
                is DataState.Success -> addressState.data?.let {
                    DataState.Success(Unit)
                }
            }

            onValueChange.invoke(addressState.dataOrNull)
        }
    }
}
