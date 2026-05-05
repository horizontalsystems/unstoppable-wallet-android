package io.horizontalsystems.bankwallet.modules.restoreconfig

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.serializers.BlockchainTypeSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class BirthdayHeightConfig(@Serializable(with = BlockchainTypeSerializer::class) val blockchainType: BlockchainType) : HSScreen() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val resultEventBus = LocalResultEventBus.current
        RestoreBirthdayHeightScreen(
            blockchainType = blockchainType,
            onCloseWithResult = { config -> closeWithConfig(config, navController, resultEventBus) },
            onCloseClick = { close(navController, resultEventBus) }
        )
    }

    private fun closeWithConfig(
        config: BirthdayHeightConfig,
        navController: NavBackStack<HSScreen>,
        resultEventBus: ResultEventBus
    ) {
        resultEventBus.sendResult(Result(config))
        navController.removeLastOrNull()
    }

    private fun close(navController: NavBackStack<HSScreen>, resultEventBus: ResultEventBus) {
        resultEventBus.sendResult(Result(null))
        navController.removeLastOrNull()
    }

    @Parcelize
    data class Result(val config: BirthdayHeightConfig?) : Parcelable
}
