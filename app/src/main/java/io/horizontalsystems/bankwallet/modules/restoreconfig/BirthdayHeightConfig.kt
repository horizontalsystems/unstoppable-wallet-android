package io.horizontalsystems.bankwallet.modules.restoreconfig

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.enablecoin.restoresettings.BirthdayHeightConfig
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class BirthdayHeightConfig(val blockchainType: BlockchainType) : BaseComposeFragment() {

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
