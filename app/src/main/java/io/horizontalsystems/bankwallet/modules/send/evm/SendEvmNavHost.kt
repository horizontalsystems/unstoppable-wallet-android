package io.horizontalsystems.bankwallet.modules.send.evm

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.address.AddressParserModule
import io.horizontalsystems.bankwallet.modules.address.AddressParserViewModel
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.sendtokenselect.PrefilledData
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun SendEvmNavHost(
    title: String,
    fragmentNavController: NavController,
    amountInputModeViewModel: AmountInputModeViewModel,
    prefilledData: PrefilledData?,
    wallet: Wallet,
    predefinedAddress: String?,
) {
    val navController = rememberNavController()
    val sendEvmViewModel = viewModel<SendEvmViewModel>(
        factory = SendEvmModule.Factory(wallet, predefinedAddress)
    )

    val paymentAddressViewModel = viewModel<AddressParserViewModel>(
        factory = AddressParserModule.Factory(wallet.token, prefilledData?.amount)
    )
    ComposeAppTheme {
        NavHost(
            navController = navController,
            startDestination = "page1",
        ) {
            composable("page1") {
                SendEvmEnterAddressScreen(
                    viewModel = sendEvmViewModel,
                    navController = fragmentNavController,
                    prefilledData = prefilledData,
                    wallet = wallet,
                    paymentAddressViewModel = paymentAddressViewModel,
                    onNext = {
                        navController.navigate("page2")
                    }
                )
            }

            composablePage("page2") {
                SendEvmScreen(
                    title = title,
                    navController = fragmentNavController,
                    amountInputModeViewModel = amountInputModeViewModel,
                    prefilledData = prefilledData,
                    wallet = wallet,
                    viewModel = sendEvmViewModel,
                    paymentAddressViewModel = paymentAddressViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }

}

