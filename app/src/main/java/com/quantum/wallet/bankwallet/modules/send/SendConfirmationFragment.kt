package com.quantum.wallet.bankwallet.modules.send

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import com.quantum.wallet.bankwallet.modules.send.monero.SendMoneroConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.monero.SendMoneroViewModel
import com.quantum.wallet.bankwallet.modules.send.solana.SendSolanaConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.solana.SendSolanaViewModel
import com.quantum.wallet.bankwallet.modules.send.stellar.SendStellarConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.stellar.SendStellarViewModel
import com.quantum.wallet.bankwallet.modules.send.ton.SendTonConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.ton.SendTonViewModel
import com.quantum.wallet.bankwallet.modules.send.tron.SendTronConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.tron.SendTronViewModel
import com.quantum.wallet.bankwallet.modules.send.zcash.SendZCashConfirmationScreen
import com.quantum.wallet.bankwallet.modules.send.zcash.SendZCashViewModel
import kotlinx.parcelize.Parcelize

class SendConfirmationFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            when (input.type) {
                Type.Bitcoin -> {
                    val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)

                    SendBitcoinConfirmationScreen(
                        navController,
                        sendBitcoinViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.ZCash -> {
                    val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment)

                    SendZCashConfirmationScreen(
                        navController,
                        sendZCashViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Tron -> {
                    val sendTronViewModel: SendTronViewModel? = try {
                        navGraphViewModels<SendTronViewModel>(R.id.sendXFragment).value
                    } catch (e: Exception) {
                        null
                    }

                    sendTronViewModel?.let { viewModel ->
                        SendTronConfirmationScreen(
                            navController,
                            viewModel,
                            input.sendEntryPointDestId
                        )
                    } ?: navController.popBackStack()
                }

                Type.Solana -> {
                    val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment)

                    SendSolanaConfirmationScreen(
                        navController,
                        sendSolanaViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Ton -> {
                    val sendTonViewModel by navGraphViewModels<SendTonViewModel>(R.id.sendXFragment)

                    SendTonConfirmationScreen(
                        navController,
                        sendTonViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Stellar -> {
                    val sendStellarViewModel by navGraphViewModels<SendStellarViewModel>(R.id.sendXFragment)

                    SendStellarConfirmationScreen(
                        navController,
                        sendStellarViewModel,
                        input.sendEntryPointDestId
                    )
                }

                Type.Monero -> {
                    val sendMoneroViewModel by navGraphViewModels<SendMoneroViewModel>(R.id.sendXFragment)

                    SendMoneroConfirmationScreen(
                        navController,
                        sendMoneroViewModel,
                        input.sendEntryPointDestId
                    )
                }
            }
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, ZCash, Solana, Tron, Ton, Stellar, Monero
    }

    @Parcelize
    data class Input(val type: Type, val sendEntryPointDestId: Int) : Parcelable
}
