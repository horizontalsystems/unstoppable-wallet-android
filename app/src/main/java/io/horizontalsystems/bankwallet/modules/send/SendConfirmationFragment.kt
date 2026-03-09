package io.horizontalsystems.bankwallet.modules.send

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationFragment.Type
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SendConfirmationScreen(
    val type: Type,
    val sendEntryPointDestId: Int
) : HSScreen(
    parentScreenClass = SendScreen::class
) {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
//        TODO("xxx nav3")
//        when (type) {
//            Type.Bitcoin -> {
//                val sendBitcoinViewModel = viewModel<SendBitcoinViewModel>()
//
//                SendBitcoinConfirmationScreen(
//                    navController,
//                    sendBitcoinViewModel,
//                    sendEntryPointDestId
//                )
//            }
//
//            Type.ZCash -> {
//                val sendZCashViewModel = viewModel<SendZCashViewModel>()
//
//                SendZCashConfirmationScreen(
//                    navController,
//                    sendZCashViewModel,
//                    sendEntryPointDestId
//                )
//            }
//
//            Type.Tron -> {
//                val sendTronViewModel = viewModel<SendTronViewModel>()
//
//                sendTronViewModel?.let { viewModel ->
//                    SendTronConfirmationScreen(
//                        navController,
//                        viewModel,
//                        sendEntryPointDestId
//                    )
//                } ?: navController.popBackStack()
//            }
//
//            Type.Solana -> {
//                val sendSolanaViewModel = viewModel<SendSolanaViewModel>()
//
//                SendSolanaConfirmationScreen(
//                    navController,
//                    sendSolanaViewModel,
//                    sendEntryPointDestId
//                )
//            }
//
//            Type.Ton -> {
//                val sendTonViewModel = viewModel<SendTonViewModel>()
//
//                SendTonConfirmationScreen(
//                    navController,
//                    sendTonViewModel,
//                    sendEntryPointDestId
//                )
//            }
//
//            Type.Stellar -> {
//                val sendStellarViewModel = viewModel<SendStellarViewModel>()
//
//                SendStellarConfirmationScreen(
//                    navController,
//                    sendStellarViewModel,
//                    sendEntryPointDestId
//                )
//            }
//
//            Type.Monero -> {
//                val sendMoneroViewModel = viewModel<SendMoneroViewModel>()
//
//                SendMoneroConfirmationScreen(
//                    navController,
//                    sendMoneroViewModel,
//                    sendEntryPointDestId
//                )
//            }
//        }
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
