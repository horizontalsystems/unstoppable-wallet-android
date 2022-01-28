package io.horizontalsystems.bankwallet.modules.address

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(modifier: Modifier, coinCode: String, onValueChange: (Address?) -> Unit) {
    val viewModel = viewModel<AddressViewModel>(factory = AddressInputModule.Factory(coinCode))

    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    FormsInput(
        modifier = modifier,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        error = error,
        qrScannerEnabled = true,
        onValueChange = {
            scope.launch {
                val addressState = viewModel.parseAddress(it)
                error = addressState.errorOrNull?.convertedError?.localizedMessage
                onValueChange.invoke(addressState.dataOrNull)
            }
        }
    )
}
