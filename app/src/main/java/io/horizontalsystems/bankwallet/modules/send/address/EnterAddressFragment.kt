package io.horizontalsystems.bankwallet.modules.send.address

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R.string.Button_Next
import io.horizontalsystems.bankwallet.R.string.Send_EnterAddress
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.SendScreen
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class EnterAddressScreen(
    val wallet: Wallet,
    val title: String,
    val sendEntryPointDestId: KClass<out HSScreen>? = null,
    val address: String? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal? = null,
    val memo: String? = null,
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        EnterAddressScreen(
            backStack = backStack,
            token = wallet.token,
            title = stringResource(Send_EnterAddress),
            buttonTitle = stringResource(Button_Next),
            allowNull = false,
            initialAddress = address
        ) { address, risky ->
            address?.let {
                backStack.add(SendScreen(
                    wallet = wallet,
                    sendEntryPointDestId = sendEntryPointDestId ?: EnterAddressScreen::class,
                    title = title,
                    address = it,
                    riskyAddress = risky,
                    amount = amount,
                    memo = memo,
                ))
            }
        }

    }
}

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
//        withInput<Input>(navController) { input ->
//            EnterAddressScreen(
//                navController = navController,
//                token = input.wallet.token,
//                title = stringResource(Send_EnterAddress),
//                buttonTitle = stringResource(Button_Next),
//                allowNull = false,
//                initialAddress = input.address
//            ) { address, risky ->
//                address?.let {
//                    navController.slideFromRight(
//                        sendXFragment,
//                        Input(
//                            wallet = input.wallet,
//                            sendEntryPointDestId = input.sendEntryPointDestId ?: enterAddressFragment,
//                            title = input.title,
//                            address = it,
//                            riskyAddress = risky,
//                            amount = input.amount,
//                            memo = input.memo,
//                        )
//                    )
//                }
//            }
//        }
    }

    @Parcelize
    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: Int? = null,
        val address: String? = null,
        val amount: BigDecimal? = null,
        val memo: String? = null,
    ) : Parcelable
}
