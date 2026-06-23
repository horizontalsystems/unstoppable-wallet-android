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
data class BirthdayHeightConfigPage(val blockchainType: BlockchainType) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        RestoreBirthdayHeightScreen(
            blockchainType = blockchainType,
            onCloseWithResult = { config -> closeWithConfig(config, navigation, resultEventBus) },
            onCloseClick = { close(navigation, resultEventBus) }
        )
    }

    private fun closeWithConfig(
        config: BirthdayHeightConfig,
        navigation: HSNavigation,
        resultEventBus: ResultEventBus
    ) {
        resultEventBus.sendResult(Result(config))
        navigation.removeLastOrNull()
    }

    private fun close(navigation: HSNavigation, resultEventBus: ResultEventBus) {
        resultEventBus.sendResult(Result(null))
        navigation.removeLastOrNull()
    }

    @Parcelize
    data class Result(val config: BirthdayHeightConfig?) : Parcelable
}
