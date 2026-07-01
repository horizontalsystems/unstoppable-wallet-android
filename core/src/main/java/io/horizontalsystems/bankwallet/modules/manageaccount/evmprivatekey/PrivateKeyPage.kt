package io.horizontalsystems.bankwallet.modules.manageaccount.evmprivatekey

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccount.SecretKeyScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class PrivateKeyPage(val input: Input) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        PrivateKeyScreen(navigation, input.privateKey, input.type)
    }

    @Serializable
    data class Input(val privateKey: String, val type: Type)

    @Serializable
    enum class Type {
        Evm, Tron
    }
}

@Composable
fun PrivateKeyScreen(
    navigation: HSNavigation,
    evmPrivateKey: String,
    type: PrivateKeyPage.Type,
) {
    val title = when (type) {
        PrivateKeyPage.Type.Evm -> stringResource(R.string.EvmPrivateKey_Title)
        PrivateKeyPage.Type.Tron -> stringResource(R.string.TronPrivateKey_Title)
    }

    val statPage = when (type) {
        PrivateKeyPage.Type.Evm -> StatPage.EvmPrivateKey
        PrivateKeyPage.Type.Tron -> StatPage.TronPrivateKey
    }

    val statEntity = when (type) {
        PrivateKeyPage.Type.Evm -> StatEntity.EvmPrivateKey
        PrivateKeyPage.Type.Tron -> StatEntity.TronPrivateKey
    }

    SecretKeyScreen(
        navigation = navigation,
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
