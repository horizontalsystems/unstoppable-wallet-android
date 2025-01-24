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
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.send.evm.AddressCheck
import io.horizontalsystems.bankwallet.modules.send.evm.AddressSuggestions
import io.horizontalsystems.bankwallet.modules.send.evm.ErrorMessage
import io.horizontalsystems.bankwallet.modules.send.evm.SContact
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import kotlinx.parcelize.Parcelize

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        EnterAddressScreen(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int = 0,
        val predefinedAddress: String? = null,
        val prefilledAddressData: PrefilledData? = null
    ) : Parcelable

}

@Composable
fun EnterAddressScreen(navController: NavController, input: EnterAddressFragment.Input) {
    val viewModel = viewModel<EnterAddressViewModel>()
    val prefilledData = input.prefilledAddressData
    val wallet = input.wallet
    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, prefilledData?.amount)
    )

    val onNext = {
        navController.slideFromRight(
            R.id.sendXFragment,
            SendFragment.Input(
                wallet = wallet,
                sendEntryPointDestId = R.id.sendTokenSelectFragment,
                title = input.title,
                prefilledAddressData = prefilledData,
            )
        )
    }

    val uiState = viewModel.uiState
    val addressError = uiState.addressError
    val recent: String? = "1EzZFZhopU4vBJKLiq5kUKphXZ4M22M1QjmEdfk"
    val addressErrorMessage: ErrorMessage? = ErrorMessage(
        title = stringResource(R.string.Send_Address_ErrorMessage_PhishingDetected),
        description = stringResource(R.string.Send_Address_ErrorMessage_PhishingDetected_Description)
    )
    val contacts = listOf(
        SContact("My Wallet", "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"),
        SContact("TalgatETH", "0x95222290DD7278Aa3Ddd389Cc1E1d165CC4BAfe5"),
        SContact("Esso", "0x71C7656EC7ab88b098defB751B7401B5f6d8976F"),
        SContact("Escobar", "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"),
        SContact("Vitalik", "0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B"),
        SContact("Binance_1", "0x28C6c06298d514Db089934071355E5743bf21d60"),
        SContact("Kraken_Hot", "0x2910543Af39abA0Cd09dBb2D50200b3E800A63D2"),
        SContact("FTX_Exploiter", "0x59ABf3837Fa962d6853b4Cc0a19513AA031fd32b"),
        SContact("Coinbase_2", "0x503828976D22510aad0201ac7EC88293211D23Da"),
        SContact("Metamask_DEV", "0x9696f59E4d72E237BE84fFD425DCaD154Bf96976")
    )

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Send_EnterAddress),
                navigationIcon = {
                    HsBackButton(onClick = { navController.popBackStack() })
                },
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
                HSAddressInput(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    initial = prefilledData?.address?.let { Address(it) },
                    tokenQuery = wallet.token.tokenQuery,
                    coinCode = wallet.coin.code,
                    error = addressError,
                    textPreprocessor = paymentAddressViewModel,
                    navController = navController
                ) {
                    viewModel.onEnterAddress(it)
                }
                if (uiState.address == null) {
                    AddressSuggestions(recent, contacts)
                } else {
                    AddressCheck(false, addressErrorMessage)
                }
            }
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = onNext,
                    enabled = uiState.canBeSendToAddress
                )
            }
        }
    }
}
