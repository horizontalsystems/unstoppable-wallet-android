package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * Send screen for every blockchain type. After a successful send the flow pops back to and
 * including [sendEntryPointDestId], or this page when none is given. [prefill] seeds the
 * form from a payment link or a fixed destination. [title] replaces the plain "Send" header
 * when the flow has a more specific purpose (a donation).
 */
@Serializable
data class SendV2Page(
    val wallet: Wallet,
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>? = null,
    val prefill: Prefill? = null,
    val title: String? = null,
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<SendViewModel>(factory = SendViewModel.Factory(wallet, prefill))
        SendV2Screen(
            navigation = navigation,
            viewModel = viewModel,
            sendEntryPointDestId = sendEntryPointDestId ?: SendV2Page::class,
            prefillAddress = prefill?.address?.takeIf { !prefill.hideAddress },
            title = title,
        )
    }

    /**
     * Values the form starts with. A prefilled [address] is still confirmed through the
     * address entry screen, with its validation and checks, unless [hideAddress] is set: then
     * it is a destination the app itself chose (a donation address), taken as is and not
     * shown.
     */
    @Serializable
    data class Prefill(
        val address: String? = null,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
        val memo: String? = null,
        val hideAddress: Boolean = false,
    )
}
