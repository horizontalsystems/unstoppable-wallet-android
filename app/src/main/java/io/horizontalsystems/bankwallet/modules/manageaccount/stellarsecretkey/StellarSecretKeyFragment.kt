package io.horizontalsystems.bankwallet.modules.manageaccount.stellarsecretkey

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccount.SecretKeyScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.parcelize.Parcelize

class StellarSecretKeyFragment(val input: Input) : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        StellarSecretKeyScreen(navController, input.stellarSecretKey)
    }

    @Parcelize
    data class Input(val stellarSecretKey: String) : Parcelable
}

@Composable
fun StellarSecretKeyScreen(
    navController: NavBackStack<HSScreen>,
    stellarSecretKey: String,
) {
    SecretKeyScreen(
        navController = navController,
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
