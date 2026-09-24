package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * The step between choosing a token for a payment link and the send screen: the link's
 * address is shown for the same validation and checks a typed one gets, and Next opens
 * [SendV2Page] with it set, together with the link's amount and memo. Unlike
 * [SendAddressPage] it is a step in a flow, not an editor over the send screen, so it has a
 * back arrow and stays on the stack under the send screen.
 */
@Serializable
data class SendRecipientPage(
    val wallet: Wallet,
    val address: String,
    @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
    val memo: String? = null,
    @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>? = null,
) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        EnterAddressScreen(
            navigation = navigation,
            token = wallet.token,
            title = stringResource(R.string.Send_EnterAddress),
            buttonTitle = stringResource(R.string.Button_Next),
            allowNull = false,
            initialAddress = address,
        ) { result, risky, contactName ->
            result?.let {
                navigation.slideFromRight(
                    SendV2Page(
                        wallet = wallet,
                        sendEntryPointDestId = sendEntryPointDestId,
                        purpose = SendV2Page.Purpose.Transfer(
                            SendV2Page.Prefill(address = it, riskyAddress = risky, contactName = contactName, amount = amount, memo = memo)
                        ),
                    )
                )
            }
        }
    }
}
