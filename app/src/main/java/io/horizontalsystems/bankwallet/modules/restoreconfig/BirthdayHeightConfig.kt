package io.horizontalsystems.bankwallet.modules.restoreconfig

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object BirthdayHeightConfigScreen : HSScreen()

class BirthdayHeightConfig : BaseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.onBackPressedDispatcher?.addCallback(this) {
            close(findNavController())
        }
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val blockchainType = navController.getInput<Token>()?.blockchainType
            ?: navController.getInput<BlockchainType>()

        blockchainType?.let {
            RestoreBirthdayHeightScreen(
                blockchainType = it,
                onCloseWithResult = { config -> closeWithConfig(config, navController) },
                onCloseClick = { close(navController) }
            )
        }
    }

    private fun closeWithConfig(config: BirthdayHeightConfig, navController: NavController) {
        navController.setNavigationResultX(Result(config))
        navController.popBackStack()
    }

    private fun close(navController: NavController) {
        navController.setNavigationResultX(Result(null))
        navController.popBackStack()
    }

    @Parcelize
    data class Result(val config: BirthdayHeightConfig?) : Parcelable
}
