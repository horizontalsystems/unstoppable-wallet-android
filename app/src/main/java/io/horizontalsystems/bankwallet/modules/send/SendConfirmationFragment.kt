package io.horizontalsystems.bankwallet.modules.send

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import kotlinx.parcelize.Parcelize

class SendConfirmationFragment : BaseComposeFragment() {
    val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            when (input.type) {
                Type.Bitcoin -> {
                    val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)

                    SendBitcoinConfirmationScreen(
                        navController,
                        sendBitcoinViewModel,
                        amountInputModeViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.ZCash -> {
                    val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment)

                    SendZCashConfirmationScreen(
                        navController,
                        sendZCashViewModel,
                        amountInputModeViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Tron -> {
                    val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment)
                    SendTronConfirmationScreen(
                        navController,
                        sendTronViewModel,
                        amountInputModeViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Solana -> {
                    val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment)

                    SendSolanaConfirmationScreen(
                        navController,
                        sendSolanaViewModel,
                        amountInputModeViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Ton -> {
                    val sendTonViewModel by navGraphViewModels<SendTonViewModel>(R.id.sendXFragment)

                    SendTonConfirmationScreen(
                        navController,
                        sendTonViewModel,
                        amountInputModeViewModel,
                        input.sendEntryPointDestId
                    )
                }
            }
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, ZCash, Solana, Tron, Ton
    }

    @Parcelize
    data class Input(val type: Type, val sendEntryPointDestId: Int) : Parcelable
}
