package com.quantum.wallet.bankwallet.modules.manageaccount.stellarsecretkey

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.manageaccount.SecretKeyScreen
import kotlinx.parcelize.Parcelize

class StellarSecretKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            StellarSecretKeyScreen(navController, input.stellarSecretKey)
        }
    }

    @Parcelize
    data class Input(val stellarSecretKey: String) : Parcelable
}

@Composable
fun StellarSecretKeyScreen(
    navController: NavController,
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
