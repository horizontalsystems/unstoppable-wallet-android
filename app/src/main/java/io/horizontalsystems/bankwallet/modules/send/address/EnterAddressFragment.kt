package io.horizontalsystems.bankwallet.modules.send.address

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R.string.Button_Next
import io.horizontalsystems.bankwallet.R.string.Send_EnterAddress
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.send.SendFragment.Input
import java.math.BigDecimal
import kotlin.reflect.KClass

class EnterAddressFragment(val input: Input) : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
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
                    SendFragment(Input(
                        wallet = input.wallet,
                        sendEntryPointDestId = input.sendEntryPointDestId ?: EnterAddressFragment::class,
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

    data class Input(
        val wallet: Wallet,
        val title: String,
        val sendEntryPointDestId: KClass<out HSScreen>? = null,
        val address: String? = null,
        val amount: BigDecimal? = null,
        val memo: String? = null,
    )
}
