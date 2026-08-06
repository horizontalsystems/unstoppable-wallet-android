package io.horizontalsystems.walletkit.modules.send

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.stellar.SendStellarViewModel
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.thorchain.SendThorchainViewModel
import io.horizontalsystems.walletkit.modules.send.ton.SendTonConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.ton.SendTonViewModel
import io.horizontalsystems.walletkit.modules.send.tron.SendTronConfirmationScreen
import io.horizontalsystems.walletkit.modules.send.tron.SendTronViewModel
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class SendConfirmationPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        when (input.type) {
            Type.Bitcoin -> {
                val sendBitcoinViewModel = navigation.viewModelForScreen<SendBitcoinViewModel>(SendPage::class)

                SendBitcoinConfirmationScreen(
                    navigation,
                    sendBitcoinViewModel,
                    input.sendEntryPointDestId
                )
            }

            // Legacy: moved to SendZcashConfirmationPage in the Zcash chain module.
            // A back stack persisted before the split may still restore this entry - pop it.
            Type.ZCash -> {
                LaunchedEffect(Unit) {
                    navigation.removeLastOrNull()
                }
            }

            Type.Tron -> {
                val sendTronViewModel = navigation.viewModelForScreen<SendTronViewModel>(SendPage::class)

                SendTronConfirmationScreen(
                    navigation,
                    sendTronViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Solana -> {
                val sendSolanaViewModel = navigation.viewModelForScreen<SendSolanaViewModel>(SendPage::class)

                SendSolanaConfirmationScreen(
                    navigation,
                    sendSolanaViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Ton -> {
                val sendTonViewModel = navigation.viewModelForScreen<SendTonViewModel>(SendPage::class)

                SendTonConfirmationScreen(
                    navigation,
                    sendTonViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Stellar -> {
                val sendStellarViewModel = navigation.viewModelForScreen<SendStellarViewModel>(SendPage::class)

                SendStellarConfirmationScreen(
                    navigation,
                    sendStellarViewModel,
                    input.sendEntryPointDestId
                )
            }

            Type.Thorchain -> {
                val sendThorchainViewModel = navigation.viewModelForScreen<SendThorchainViewModel>(SendPage::class)

                SendThorchainConfirmationScreen(
                    navigation,
                    sendThorchainViewModel,
                    input.sendEntryPointDestId
                )
            }

            // Legacy: moved to SendMoneroConfirmationPage in the Monero chain module.
            // A back stack persisted before the split may still restore this entry - pop it.
            Type.Monero -> {
                LaunchedEffect(Unit) {
                    navigation.removeLastOrNull()
                }
            }

            Type.Zano -> {
                LaunchedEffect(Unit) {
                    navigation.removeLastOrNull()
                }
            }

        }
    }

    @Serializable
    enum class Type {
        Bitcoin, ZCash, Solana, Tron, Ton, Stellar, Thorchain, Monero,

        // Legacy: Zano confirmation moved to SendZanoConfirmationPage in walletkit-chain-zano.
        // Kept so a back stack persisted before the split still deserializes; renders nothing.
        Zano,
    }

    @Serializable
    data class Input(val type: Type, @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>)
}
