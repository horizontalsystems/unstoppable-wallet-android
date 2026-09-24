package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Address
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
 * including [sendEntryPointDestId], or this page when none is given. [purpose] says what the
 * flow is for and carries its inputs: a plain transfer asks for the recipient, possibly
 * seeded from a payment link, and offers every tab; a donation goes to a fixed address and
 * leaves CrossPay out.
 */
@Serializable
data class SendV2Page(
    val wallet: Wallet,
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>? = null,
    val purpose: Purpose = Purpose.Transfer(),
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<SendViewModel>(factory = SendViewModel.Factory(wallet, purpose))
        SendV2Screen(
            navigation = navigation,
            viewModel = viewModel,
            sendEntryPointDestId = sendEntryPointDestId ?: SendV2Page::class,
            purpose = purpose,
        )
    }

    /** What the send is for; each purpose carries its inputs and implies how the screen is presented. */
    @Serializable
    sealed class Purpose {
        /**
         * The user picks the recipient and every tab is offered. [prefill] seeds the form
         * from a payment link, its address already confirmed on [SendRecipientPage].
         */
        @Serializable
        data class Transfer(val prefill: Prefill? = null) : Purpose()

        /**
         * A destination the app itself chose. [address] is taken as is and not shown, the
         * header reads [title], and CrossPay, with its own token and recipient choice, is
         * left out.
         */
        @Serializable
        data class Donation(val address: String, val title: String) : Purpose()
    }

    /** Values a payment link supplies for a [Purpose.Transfer]; [address] has passed the address checks. */
    @Serializable
    data class Prefill(
        val address: Address? = null,
        val riskyAddress: Boolean = false,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
        val memo: String? = null,
    )
}
