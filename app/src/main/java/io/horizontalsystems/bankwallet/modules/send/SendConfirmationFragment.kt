package io.horizontalsystems.bankwallet.modules.send

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment.Type
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.monero.SendMoneroViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.ton.SendTonViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendConfirmationScreen(
    val type: Type,
    val sendEntryPointDestId: KClass<out HSScreen>
) : HSScreen(
    parentScreenClass = SendScreen::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        when (type) {
            Type.Bitcoin -> {
                val sendBitcoinViewModel = viewModel<SendBitcoinViewModel>()

                SendBitcoinConfirmationScreen(
                    backStack,
                    sendBitcoinViewModel,
                    sendEntryPointDestId
                )
            }

            Type.ZCash -> {
                val sendZCashViewModel = viewModel<SendZCashViewModel>()

                SendZCashConfirmationScreen(
                    backStack,
                    sendZCashViewModel,
                    sendEntryPointDestId
                )
            }

            Type.Tron -> {
                val sendTronViewModel = viewModel<SendTronViewModel>()

                SendTronConfirmationScreen(
                    backStack,
                    sendTronViewModel,
                    sendEntryPointDestId
                )
            }

            Type.Solana -> {
                val sendSolanaViewModel = viewModel<SendSolanaViewModel>()

                SendSolanaConfirmationScreen(
                    backStack,
                    sendSolanaViewModel,
                    sendEntryPointDestId
                )
            }

            Type.Ton -> {
                val sendTonViewModel = viewModel<SendTonViewModel>()

                SendTonConfirmationScreen(
                    backStack,
                    sendTonViewModel,
                    sendEntryPointDestId
                )
            }

            Type.Stellar -> {
                val sendStellarViewModel = viewModel<SendStellarViewModel>()

                SendStellarConfirmationScreen(
                    backStack,
                    sendStellarViewModel,
                    sendEntryPointDestId
                )
            }

            Type.Monero -> {
                val sendMoneroViewModel = viewModel<SendMoneroViewModel>()

                SendMoneroConfirmationScreen(
                    backStack,
                    sendMoneroViewModel,
                    sendEntryPointDestId
                )
            }
        }
    }
}

class SendConfirmationFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, ZCash, Solana, Tron, Ton, Stellar, Monero
    }

    @Parcelize
    data class Input(val type: Type, val sendEntryPointDestId: Int) : Parcelable
}
