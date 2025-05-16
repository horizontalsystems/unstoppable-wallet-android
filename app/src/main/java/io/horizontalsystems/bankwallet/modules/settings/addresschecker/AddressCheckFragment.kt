package io.horizontalsystems.bankwallet.modules.settings.addresschecker

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.modules.send.address.CoinAddressCheckScreen
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.AddressCheckBlockchainSelectorScreen
import io.horizontalsystems.bankwallet.modules.settings.addresschecker.ui.AddressCheckTokenSelectorScreen
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token

class AddressCheckFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AddressCheck(
            onCloseChecker = { navController.popBackStack() }
        )
    }
}

@Composable
fun AddressCheck(
    onCloseChecker: () -> Unit
) {
    val viewModel: AddressCheckSharedViewModel = viewModel()
    val navHostController = rememberNavController()
    NavHost(
        navController = navHostController,
        startDestination = "select_blockchain",
    ) {
        composable("select_blockchain") {
            AddressCheckBlockchainSelectorScreen(
                onSelect = { blockchain ->
                    viewModel.setBlockchain(blockchain)
                    navHostController.navigate("select_token")
                },
                onClose = onCloseChecker
            )
        }
        composablePopup("select_token") {
            AddressCheckTokenSelectorScreen(
                selectedBlockchain = viewModel.getBlockchain(),
                onSelect = { token ->
                    viewModel.setToken(token)
                    navHostController.navigate("check_address")
                },
                onBackPress = {
                    navHostController.popBackStack()
                }
            )
        }
        composablePopup("check_address") {
            CoinAddressCheckScreen(
                token = viewModel.getToken(),
                onBackPress = {
                    navHostController.popBackStack()
                },
                onClose = onCloseChecker
            )
        }
    }
}

class AddressCheckSharedViewModel : ViewModel() {
    private var blockchain: Blockchain? = null
    private var token: Token? = null

    fun getBlockchain(): Blockchain? {
        return blockchain
    }

    fun setBlockchain(blockchain: Blockchain) {
        this.blockchain = blockchain
    }

    fun getToken(): Token? {
        return token
    }

    fun setToken(token: Token) {
        this.token = token
    }
}