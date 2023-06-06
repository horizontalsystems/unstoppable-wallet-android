package io.horizontalsystems.bankwallet.modules.address

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputAddress
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessor
import io.horizontalsystems.bankwallet.ui.compose.components.TextPreprocessorImpl
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    initial: Address? = null,
    tokenQuery: TokenQuery,
    coinCode: String,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    navController: NavController,
    onStateChange: ((DataState<Address>?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null
) {
    val viewModel = viewModel<AddressViewModel>(
            factory = AddressInputModule.FactoryToken(tokenQuery, coinCode),
            key = "address_view_model_${tokenQuery.id}"
    )

    HSAddressInput(
        modifier = modifier,
        viewModel = viewModel,
        initial = initial,
        error = error,
        textPreprocessor = textPreprocessor,
        navController = navController,
        onStateChange = onStateChange,
        onValueChange = onValueChange
    )
}

val DataStateAddressSaver: Saver<DataState<Address>?, Any> = run {
    val addressKey = "data"
    val loadingKey = "loading"
    val errorKey = "error"
    mapSaver(
        save = {
            val address = it?.dataOrNull
            val error = (it?.errorOrNull as? Parcelable)
            val loading = it?.loading

            mapOf(
                addressKey to address,
                loadingKey to loading,
                errorKey to error,
            )
        },
        restore = {
            val address = it[addressKey] as? Address
            val loading = it[loadingKey] as? Boolean
            val error = it[errorKey] as? Throwable

            when {
                address != null -> DataState.Success(address)
                error != null -> DataState.Error(error)
                loading == true -> DataState.Loading
                else -> null
            }
        }
    )
}
@Composable
fun HSAddressInput(
    modifier: Modifier = Modifier,
    viewModel: AddressViewModel,
    initial: Address? = null,
    error: Throwable? = null,
    textPreprocessor: TextPreprocessor = TextPreprocessorImpl,
    navController: NavController,
    onStateChange: ((DataState<Address>?) -> Unit)? = null,
    onValueChange: ((Address?) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var addressState by rememberSaveable(stateSaver = DataStateAddressSaver) {
        mutableStateOf(initial?.let { DataState.Success(it) })
    }
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

    FormsInputAddress(
        modifier = modifier,
        initial = initial?.title,
        hint = stringResource(id = R.string.Watch_Address_Hint),
        state = inputState,
        textPreprocessor = textPreprocessor,
        onChangeFocus = {
            isFocused = it
        },
        navController = navController,
        chooseContactEnable = viewModel.hasContacts(),
        blockchainType = viewModel.blockchainType,
    ) {
        parseAddressJob?.cancel()
        parseAddressJob = scope.launch {
            addressState = DataState.Loading

            val state = try {
                DataState.Success(viewModel.parseAddress(it))
            } catch (e: AddressValidationException.Blank) {
                null
            } catch (e: AddressValidationException) {
                DataState.Error(e)
            }

            ensureActive()
            addressState = state
            onValueChange?.invoke(addressState?.dataOrNull)
            onStateChange?.invoke(addressState)
        }
    }
}

private fun getFocusedState(state: DataState<Address>?): DataState<Address>? {
    return if (state is DataState.Error &&
        (state.error !is AddressValidationException.Invalid && state.error !is FormsInputStateWarning)) {
        null
    } else {
        state
    }
}
