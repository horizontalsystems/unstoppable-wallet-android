package io.horizontalsystems.bankwallet.modules.restoreaccount

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.requireInput
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.ManageWalletsScreen
import io.horizontalsystems.bankwallet.modules.restoreconfig.RestoreBirthdayHeightScreen
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class RestoreFromPasskeyFragment : BaseComposeFragment(screenshotEnabled = false) {
    @Composable
    override fun GetContent(navController: NavController) {
        RestoreFromPasskeyNavHost(navController, navController.requireInput())
    }

    @Parcelize
    data class Input(
        val popOffOnSuccess: Int,
        val popOffInclusive: Boolean,
        val entropy: ByteArray,
        val accountName: String?
    ) : Parcelable
}

@Composable
private fun RestoreFromPasskeyNavHost(
    fragmentNavController: NavController,
    input: RestoreFromPasskeyFragment.Input,
) {
    val popUpToInclusiveId = input.popOffOnSuccess
    val inclusive = input.popOffInclusive

    val viewModel = viewModel<RestoreFromPasskeyViewModel>(
        factory = RestoreFromPasskeyViewModel.Factory()
    )

    val navController = rememberNavController()
    val mainViewModel: RestoreViewModel = viewModel {
        val accountType = viewModel.getAccountType(input.entropy)
        val accountName = viewModel.getAccountName(input.accountName)

        RestoreViewModel(
            accountType = accountType,
            accountName = accountName,
            manualBackup = true,
            fileBackup = false,
            statPage = StatPage.ImportWalletFromPasskey
        )
    }

    NavHost(
        navController = navController,
        startDestination = "restore_select_coins",
    ) {
        composablePage("restore_select_coins") {
            ManageWalletsScreen(
                mainViewModel = mainViewModel,
                openBirthdayHeightConfigure = { token ->
                    when (token.blockchainType) {
                        BlockchainType.Zcash -> navController.navigate("zcash_configure")
                        BlockchainType.Monero -> navController.navigate("monero_configure")
                        else -> Unit
                    }
                },
                onBackClick = { navController.popBackStack() },
                onFinish = {
                    fragmentNavController.popBackStack(popUpToInclusiveId, inclusive)
                }
            )
        }
        composablePage("zcash_configure") {
            RestoreBirthdayHeightScreen(
                blockchainType = BlockchainType.Zcash,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
        composablePage("monero_configure") {
            RestoreBirthdayHeightScreen(
                blockchainType = BlockchainType.Monero,
                onCloseWithResult = { config ->
                    mainViewModel.setBirthdayHeightConfig(config)
                    navController.popBackStack()
                },
                onCloseClick = {
                    mainViewModel.cancelBirthdayHeightConfig = true
                    navController.popBackStack()
                }
            )
        }
    }
}
