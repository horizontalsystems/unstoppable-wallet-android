package io.horizontalsystems.bankwallet.modules.send

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
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
import io.horizontalsystems.bankwallet.modules.send.zano.SendZanoConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.zano.SendZanoViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.bankwallet.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        when (input.type) {
            Type.Bitcoin -> {
                val sendBitcoinViewModel = navController.viewModelForScreen<SendBitcoinViewModel>(SendPage::class)

                SendBitcoinConfirmationScreen(
                    navController,
                    sendBitcoinViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.ZCash -> {
                val sendZCashViewModel = navController.viewModelForScreen<SendZCashViewModel>(SendPage::class)

                SendZCashConfirmationScreen(
                    navController,
                    sendZCashViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Tron -> {
                val sendTronViewModel = navController.viewModelForScreen<SendTronViewModel>(SendPage::class)

                SendTronConfirmationScreen(
                    navController,
                    sendTronViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Solana -> {
                val sendSolanaViewModel = navController.viewModelForScreen<SendSolanaViewModel>(SendPage::class)

                SendSolanaConfirmationScreen(
                    navController,
                    sendSolanaViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Ton -> {
                val sendTonViewModel = navController.viewModelForScreen<SendTonViewModel>(SendPage::class)

                SendTonConfirmationScreen(
                    navController,
                    sendTonViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Stellar -> {
                val sendStellarViewModel = navController.viewModelForScreen<SendStellarViewModel>(SendPage::class)

                SendStellarConfirmationScreen(
                    navController,
                    sendStellarViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Monero -> {
                val sendMoneroViewModel = navController.viewModelForScreen<SendMoneroViewModel>(SendPage::class)

                SendMoneroConfirmationScreen(
                    navController,
                    sendMoneroViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Zano -> {
                val sendZanoViewModel = navController.viewModelForScreen<SendZanoViewModel>(SendPage::class)

                SendZanoConfirmationScreen(
                    navController,
                    sendZanoViewModel,
                    input.sendEntryPointDestId
                )
            }
        }
    }

    @Serializable
    enum class Type {
        Bitcoin, ZCash, Solana, Tron, Ton, Stellar, Monero, Zano
    }

    @Serializable
    data class Input(val type: Type, @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>)
}
