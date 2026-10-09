package io.horizontalsystems.walletkit.modules.nearaccount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.ui.compose.components.FormsInput
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.HSScaffold
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import io.horizontalsystems.walletkit.uiv3.components.info.TextBlock
import kotlinx.serialization.Serializable

/** Manual entry for a named account the account search did not find. */
@Serializable
data class NearAccountNamePage(val publicKey: String) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<NearAccountNameViewModel>(factory = NearAccountNameViewModel.Factory(publicKey))
        val resultEventBus = LocalResultEventBus.current
        val uiState = viewModel.uiState

        LaunchedEffect(uiState.accountId) {
            uiState.accountId?.let {
                resultEventBus.sendResult(Result(it))
                navigation.removeLastOrNull()
            }
        }

        HSScaffold(
            title = stringResource(R.string.NearAccountName_Title),
            onBack = { navigation.removeLastOrNull() },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    TextBlock(stringResource(R.string.NearAccountName_Description))
                    FormsInput(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        hint = "alice.near",
                        singleLine = true,
                        state = uiState.state,
                        qrScannerEnabled = false,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        onValueChange = viewModel::onEnterText,
                    )
                }
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    HSButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        title = stringResource(R.string.Button_Add),
                        loadingIndicator = uiState.state?.loading == true,
                        enabled = uiState.addEnabled,
                        onClick = viewModel::onAdd,
                    )
                    VSpacer(16.dp)
                }
            }
        }
    }

    data class Result(val accountId: String)
}
