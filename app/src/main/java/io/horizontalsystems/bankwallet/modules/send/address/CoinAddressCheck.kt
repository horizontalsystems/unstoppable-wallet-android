package io.horizontalsystems.bankwallet.modules.send.address

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.StellarAssetAdapter
import io.horizontalsystems.bankwallet.core.address.AddressCheckResult
import io.horizontalsystems.bankwallet.core.address.AddressCheckType
import io.horizontalsystems.bankwallet.modules.send.address.ui.CheckAddressInput
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.TextImportantError
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinAddressCheckScreen(
    token: Token?,
    onBackPress: () -> Unit,
    onClose: () -> Unit,
) {
    if (token == null) {
        val context = LocalContext.current
        Toast.makeText(context, "Error: Token is null", Toast.LENGTH_SHORT).show()
        onClose()
        return
    }

    val viewModel = viewModel<EnterAddressViewModel>(
        factory = EnterAddressViewModel.Factory(
            token = token,
            address = null,
            addressCheckerSkippable = false
        )
    )

    val coroutineScope = rememberCoroutineScope()
    val infoModalBottomSheetState =
        androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var checkTypeInfoBottomSheet by remember { mutableStateOf<AddressCheckType?>(null) }

    val uiState = viewModel.uiState
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Send_EnterAddress),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )
        },
    ) { innerPaddings ->
        Column(
            modifier = Modifier
                .padding(innerPaddings)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                CheckAddressInput(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    value = uiState.value,
                    hint = stringResource(id = R.string.Send_Hint_Address),
                    state = uiState.inputState,
                ) {
                    viewModel.onEnterAddress(it)
                }

                if (uiState.value.isBlank()) {
                    AddressSuggestions(
                        uiState.recentAddress,
                        uiState.recentContact,
                        uiState.contacts
                    ) {
                        viewModel.onEnterAddress(it)
                    }
                } else {
                    AddressCheck(
                        uiState.addressValidationInProgress,
                        uiState.addressValidationError,
                        uiState.checkResults,
                    ) { checkType ->
                        if (uiState.checkResults.any { it.value.checkResult == AddressCheckResult.NotAllowed }) {
                            viewModel.onEnterAddress(uiState.value)
                        } else {
                            checkTypeInfoBottomSheet = checkType
                            coroutineScope.launch {
                                infoModalBottomSheetState.show()
                            }
                        }
                    }
                    uiState.addressValidationError?.let {
                        ValidationError(it)
                    }
                }
            }
        }
    }

    checkTypeInfoBottomSheet?.let { checkType ->
        AddressEnterInfoBottomSheet(
            checkType = checkType,
            bottomSheetState = infoModalBottomSheetState,
            hideBottomSheet = {
                coroutineScope.launch {
                    infoModalBottomSheetState.hide()
                }
                checkTypeInfoBottomSheet = null
            }
        )
    }
}

@Composable
private fun ValidationError(addressValidationError: Throwable) {
    TextImportantError(
        modifier = Modifier.padding(horizontal = 16.dp),
        icon = R.drawable.ic_attention_20,
        title = stringResource(R.string.SwapSettings_Error_InvalidAddress),
        text = addressValidationError.getErrorMessage()
            ?: stringResource(R.string.SwapSettings_Error_InvalidAddress)
    )
    VSpacer(32.dp)
}

@Composable
private fun Throwable.getErrorMessage() = when (this) {
    is StellarAssetAdapter.NoTrustlineError -> {
        stringResource(R.string.Error_AssetNotEnabled, code)
    }

    else -> this.message
}