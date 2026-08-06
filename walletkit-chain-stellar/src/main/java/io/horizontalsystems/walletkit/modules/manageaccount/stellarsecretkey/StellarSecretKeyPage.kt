package io.horizontalsystems.walletkit.modules.manageaccount.stellarsecretkey

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.stats.StatEntity
import io.horizontalsystems.walletkit.core.stats.StatEvent
import io.horizontalsystems.walletkit.core.stats.StatPage
import io.horizontalsystems.walletkit.core.stats.stat
import io.horizontalsystems.walletkit.modules.manageaccount.SecretKeyScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class StellarSecretKeyPage(val input: Input) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        StellarSecretKeyScreen(navigation, input.stellarSecretKey)
    }

    @Serializable
    data class Input(val stellarSecretKey: String)
}

@Composable
fun StellarSecretKeyScreen(
    navigation: HSNavigation,
    stellarSecretKey: String,
) {
    SecretKeyScreen(
        navigation = navigation,
        secretKey = stellarSecretKey,
        title = stringResource(R.string.StellarSecretKey_Title),
        hideScreenText = stringResource(R.string.StellarSecretKey_ShowSecretKey),
        onCopyKey = {
            stat(
                page = StatPage.StellarSecretKey,
                event = StatEvent.Copy(StatEntity.StellarSecretKey)
            )
        },
        onOpenFaq = {
            stat(
                page = StatPage.StellarSecretKey,
                event = StatEvent.Open(StatPage.Info)
            )
        },
        onToggleHidden = {
            stat(page = StatPage.StellarSecretKey, event = StatEvent.ToggleHidden)
        }
    )
}
