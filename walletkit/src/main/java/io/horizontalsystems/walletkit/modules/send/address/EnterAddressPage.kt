package io.horizontalsystems.walletkit.modules.send.address

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R.string.Button_Next
import io.horizontalsystems.walletkit.R.string.Send_EnterAddress
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.SendPage
import io.horizontalsystems.walletkit.modules.send.SendPage.Input
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class EnterAddressPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        EnterAddressScreen(
            navigation = navigation,
            token = input.wallet.token,
            title = stringResource(Send_EnterAddress),
            buttonTitle = stringResource(Button_Next),
            allowNull = false,
            initialAddress = input.address
        ) { address, risky ->
            address?.let {
                navigation.slideFromRight(
                    SendPage(Input(
                        wallet = input.wallet,
                        sendEntryPointDestId = input.sendEntryPointDestId ?: EnterAddressPage::class,
                        title = input.title,
                        address = it,
                        riskyAddress = risky,
                        amount = input.amount,
                        memo = input.memo,
                    ))
                )
            }
        }
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val title: String,
        @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>? = null,
        val address: String? = null,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
        val memo: String? = null,
    )
}
