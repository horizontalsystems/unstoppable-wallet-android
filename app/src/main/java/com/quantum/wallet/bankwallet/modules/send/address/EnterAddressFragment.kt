package com.quantum.wallet.bankwallet.modules.send.address

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R.id.enterAddressFragment
import com.quantum.wallet.bankwallet.R.id.sendXFragment
import com.quantum.wallet.bankwallet.R.string.Button_Next
import com.quantum.wallet.bankwallet.R.string.Send_EnterAddress
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.enteraddress.EnterAddressScreen
import com.quantum.wallet.bankwallet.modules.send.SendFragment.Input
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

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
