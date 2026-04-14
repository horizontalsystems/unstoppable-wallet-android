package com.quantum.wallet.bankwallet.modules.manageaccount.evmprivatekey

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
