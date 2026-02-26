package io.horizontalsystems.bankwallet.modules.send.address

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R.id.enterAddressFragment
import io.horizontalsystems.bankwallet.R.id.sendXFragment
import io.horizontalsystems.bankwallet.R.string.Button_Next
import io.horizontalsystems.bankwallet.R.string.Send_EnterAddress
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.SendFragment.Input
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data object EnterAddressScreen : HSScreen()

class EnterAddressFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            EnterAddressScreen(
                navController = navController,
                token = input.wallet.token,
                title = stringResource(Send_EnterAddress),
                buttonTitle = stringResource(Button_Next),
                allowNull = false,
                initialAddress = input.address
            ) { address, risky ->
                address?.let {
                    navController.slideFromRight(
                        sendXFragment,
                        Input(
                            wallet = input.wallet,
                            sendEntryPointDestId = input.sendEntryPointDestId ?: enterAddressFragment,
                            title = input.title,
                            address = it,
                            riskyAddress = risky,
                            amount = input.amount,
                            memo = input.memo,
                        )
                    )
                }
            }
        }
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
