package io.horizontalsystems.walletkit.modules.send.zcash

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendZcashConfirmationPage(
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>,
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val sendZCashViewModel = navigation.viewModelForScreen<SendZCashViewModel>(SendPage::class)

        SendZCashConfirmationScreen(
            navigation,
            sendZCashViewModel,
            sendEntryPointDestId
        )
    }
}
