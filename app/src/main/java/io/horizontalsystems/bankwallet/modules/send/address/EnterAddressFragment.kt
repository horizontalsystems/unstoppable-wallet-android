package io.horizontalsystems.bankwallet.modules.send.address

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddress
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            EnterAddressScreenM(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int? = null,
        val address: String? = null,
        val amount: BigDecimal? = null,
        val memo: String? = null,
    ) : Parcelable
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterAddressScreenM(navController: NavController, input: EnterAddressFragment.Input) {
    val wallet = input.wallet
    val amount = input.amount

    var hasValidationError by remember { mutableStateOf(false) }
    var riskyAddress by remember { mutableStateOf(false) }
    var address by remember { mutableStateOf<Address?>(null) }
    var validationInProgress by remember { mutableStateOf(false) }

    HSScaffold(
        title = stringResource(R.string.Send_EnterAddress),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                EnterAddress(
                    modifier =  Modifier,
                    navController = navController,
                    token = wallet.token,
                    initialAddress = input.address,
                    onValueChange = {
                        address = it
                    },
                    onValidationError = { error ->
                        hasValidationError = error != null
                    },
                    onRiskyAddress = {
                        riskyAddress = it
                    },
                    onValidationInProgress = {
                        validationInProgress = it
                    }
                )
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = if (hasValidationError)
                        stringResource(R.string.Send_Address_Error_InvalidAddress)
                    else
                        stringResource(R.string.Button_Next),
                    onClick = {
                        address?.let {
                            navController.slideFromRight(
                                R.id.sendXFragment,
                                SendFragment.Input(
                                    wallet = wallet,
                                    sendEntryPointDestId = input.sendEntryPointDestId
                                        ?: R.id.enterAddressFragment,
                                    title = input.title,
                                    address = it,
                                    riskyAddress = riskyAddress,
                                    amount = amount,
                                    memo = input.memo,
                                )
                            )
                        }
                    },
                    enabled = address != null && !validationInProgress && !hasValidationError
                )
            }
        }
    }
}
