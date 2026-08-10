package io.horizontalsystems.walletkit.modules.send

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeModule
import io.horizontalsystems.walletkit.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.send.tron.SendTronModule
import io.horizontalsystems.walletkit.modules.send.tron.SendTronScreen
import io.horizontalsystems.walletkit.modules.send.tron.SendTronViewModel
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.ChainSendScreenArgs
import io.horizontalsystems.walletkit.serializers.BigDecimalSerializer
import io.horizontalsystems.walletkit.serializers.HSScreenKClassSerializer
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.reflect.KClass

@Serializable
data class SendPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val wallet = input.wallet
        val title = input.title
        val sendEntryPointDestId = input.sendEntryPointDestId
        val address = input.address
        val riskyAddress = input.riskyAddress
        val hideAddress = input.hideAddress
        val amount = input.amount
        val memo = input.memo

        val amountInputModeViewModel = viewModel<AmountInputModeViewModel>(
            factory = AmountInputModeModule.Factory(wallet.coin.uid)
        )

        when (wallet.token.blockchainType) {





            BlockchainType.Tron -> {
                val factory = SendTronModule.Factory(wallet, address, hideAddress)
                val sendTronViewModel = viewModel<SendTronViewModel>(factory = factory)
                SendTronScreen(
                    title = title,
                    navigation = navigation,
                    viewModel = sendTronViewModel,
                    amountInputModeViewModel = amountInputModeViewModel,
                    sendEntryPointDestId = sendEntryPointDestId,
                    amount = amount,
                    riskyAddress = riskyAddress
                )
            }




            else -> {
                ChainRegistry[wallet.token.blockchainType]?.SendScreen(
                    ChainSendScreenArgs(
                        wallet = wallet,
                        title = title,
                        navigation = navigation,
                        amountInputModeViewModel = amountInputModeViewModel,
                        sendEntryPointDestId = sendEntryPointDestId,
                        address = address,
                        amount = amount,
                        memo = memo,
                        hideAddress = hideAddress,
                        riskyAddress = riskyAddress,
                    )
                )
            }
        }
    }

    @Serializable
    data class Input(
        val wallet: Wallet,
        val title: String,
        @Serializable(with = HSScreenKClassSerializer::class) val sendEntryPointDestId: KClass<out HSPage>,
        val address: Address,
        val riskyAddress: Boolean = false,
        @Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal? = null,
        val hideAddress: Boolean = false,
        val memo: String? = null,
    )
}
