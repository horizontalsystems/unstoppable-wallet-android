package io.horizontalsystems.bankwallet.modules.manageaccount.evmprivatekey

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccount.SecretKeyScreen
import kotlinx.parcelize.Parcelize

class EvmPrivateKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            EvmPrivateKeyScreen(navController, input.evmPrivateKey)
        }
    }

    @Parcelize
    data class Input(val evmPrivateKey: String) : Parcelable
}

@Composable
fun EvmPrivateKeyScreen(
    navController: NavController,
    evmPrivateKey: String,
) {
    SecretKeyScreen(
        navController = navController,
        secretKey = evmPrivateKey,
        title = stringResource(R.string.EvmPrivateKey_Title),
        hideScreenText = stringResource(R.string.EvmPrivateKey_ShowPrivateKey),
        onCopyKey = {
            stat(
                page = StatPage.EvmPrivateKey,
                event = StatEvent.Copy(StatEntity.EvmPrivateKey)
            )
        },
        onOpenFaq = {
            stat(
                page = StatPage.EvmPrivateKey,
                event = StatEvent.Open(StatPage.Info)
            )
        },
        onToggleHidden = {
            stat(page = StatPage.EvmPrivateKey, event = StatEvent.ToggleHidden)
        }
    )
}
