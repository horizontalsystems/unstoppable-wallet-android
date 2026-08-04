package io.horizontalsystems.walletkit.modules.send.monero

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendMoneroConfirmationPage(
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>,
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val sendMoneroViewModel = navigation.viewModelForScreen<SendMoneroViewModel>(SendPage::class)

        SendMoneroConfirmationScreen(
            navigation,
            sendMoneroViewModel,
            sendEntryPointDestId
        )
    }
}
