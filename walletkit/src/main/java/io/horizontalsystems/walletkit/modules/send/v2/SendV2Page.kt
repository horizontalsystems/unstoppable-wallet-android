package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Send screen for every blockchain type. After a successful send the flow pops back to and
 * including [sendEntryPointDestId], or this page when none is given.
 */
@Serializable
data class SendV2Page(
    val wallet: Wallet,
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>? = null,
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<SendViewModel>(factory = SendViewModel.Factory(wallet))
        SendV2Screen(navigation, viewModel, sendEntryPointDestId ?: SendV2Page::class)
    }
}
