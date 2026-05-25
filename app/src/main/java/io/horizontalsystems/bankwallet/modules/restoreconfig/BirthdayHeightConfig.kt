package io.horizontalsystems.bankwallet.modules.restoreconfig

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class BirthdayHeightConfig(val blockchainType: BlockchainType) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        RestoreBirthdayHeightScreen(
            blockchainType = blockchainType,
            onCloseWithResult = { config -> closeWithConfig(config, navController, resultEventBus) },
            onCloseClick = { close(navController, resultEventBus) }
        )
    }

    private fun closeWithConfig(
        config: BirthdayHeightConfig,
        navController: HSNavigation,
        resultEventBus: ResultEventBus
    ) {
        resultEventBus.sendResult(Result(config))
        navController.removeLastOrNull()
    }

    private fun close(navController: HSNavigation, resultEventBus: ResultEventBus) {
        resultEventBus.sendResult(Result(null))
        navController.removeLastOrNull()
    }

    @Parcelize
    data class Result(val config: BirthdayHeightConfig?) : Parcelable
}
