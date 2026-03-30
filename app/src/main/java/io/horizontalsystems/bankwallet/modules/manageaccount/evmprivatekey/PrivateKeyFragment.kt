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

class PrivateKeyFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            PrivateKeyScreen(navController, input.privateKey, input.type)
        }
    }

    @Parcelize
    data class Input(val privateKey: String, val type: Type) : Parcelable

    @Parcelize
    enum class Type : Parcelable {
        Evm, Tron
    }
}

@Composable
fun PrivateKeyScreen(
    navController: NavController,
    evmPrivateKey: String,
    type: PrivateKeyFragment.Type,
) {
    val title = when (type) {
        PrivateKeyFragment.Type.Evm -> stringResource(R.string.EvmPrivateKey_Title)
        PrivateKeyFragment.Type.Tron -> stringResource(R.string.TronPrivateKey_Title)
    }

    val statPage = when (type) {
        PrivateKeyFragment.Type.Evm -> StatPage.EvmPrivateKey
        PrivateKeyFragment.Type.Tron -> StatPage.TronPrivateKey
    }

    val statEntity = when (type) {
        PrivateKeyFragment.Type.Evm -> StatEntity.EvmPrivateKey
        PrivateKeyFragment.Type.Tron -> StatEntity.TronPrivateKey
    }

    SecretKeyScreen(
        navController = navController,
        secretKey = evmPrivateKey,
        title = title,
        hideScreenText = stringResource(R.string.EvmPrivateKey_ShowPrivateKey),
        onCopyKey = {
            stat(
                page = statPage,
                event = StatEvent.Copy(statEntity)
            )
        },
        onOpenFaq = {
            stat(
                page = statPage,
                event = StatEvent.Open(StatPage.Info)
            )
        },
        onToggleHidden = {
            stat(page = statPage, event = StatEvent.ToggleHidden)
        }
    )
}
