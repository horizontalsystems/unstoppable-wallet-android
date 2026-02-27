package io.horizontalsystems.bankwallet.modules.restoreconfig

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class BirthdayHeightConfigScreen(
    @Serializable(with = TokenSerializer::class)
    val token: Token
) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val onBack: () -> Unit = {
            resultBus.sendResult(result = Result(null))
            backStack.removeLastOrNull()
        }

        BackHandler(onBack = onBack)
        RestoreBirthdayHeightScreen(
            blockchainType = token.blockchainType,
            onCloseWithResult = { config ->
                resultBus.sendResult(result = Result(config))
                backStack.removeLastOrNull()
            },
            onCloseClick = onBack
        )
    }

    data class Result(val config: BirthdayHeightConfig?)
}

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
