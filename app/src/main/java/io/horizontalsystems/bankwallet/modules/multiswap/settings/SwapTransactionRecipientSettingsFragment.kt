package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddress
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SwapTransactionRecipientSettingsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) {
            SwapRecipientSettingsScreen(navController, it.token, it.recipient)
        }
    }

    @Parcelize
    data class Input(val token: Token, val recipient: Address?) : Parcelable

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}

@Composable
fun SwapRecipientSettingsScreen(
    navController: NavController,
    token: Token,
    recipient: Address?
) {
    var hasValidationError by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf<Address?>(null) }
    var validationInProgress by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.SendEvmSettings_SetRecipient),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                    .fillMaxSize()
            ) {
                EnterAddress(
                    modifier = Modifier,
                    navController = navController,
                    token = token,
                    initialAddress = recipient?.hex,
                    onValueChange = {
                        address = it
                    },
                    onValidationError = { error ->
                        Log.e("AAA", "onValidationError: $error")
                        hasValidationError = error != null
                    },
                    onValidationInProgress = {
                        validationInProgress = it
                    }
                )

                VSpacer(32.dp)
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = if (hasValidationError)
                        stringResource(R.string.Send_Address_Error_InvalidAddress)
                    else
                        stringResource(R.string.Button_Apply),
                    onClick = {
                        navController.setNavigationResultX(SwapTransactionRecipientSettingsFragment.Result(address))
                        navController.popBackStack()
                    },
                    enabled = !validationInProgress && !hasValidationError
                )
            }
        }
    }
}
