package io.horizontalsystems.bankwallet.modules.send

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.binance.SendBinanceViewModel
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.solana.SendSolanaViewModel
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.tron.SendTronViewModel
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashConfirmationScreen
import io.horizontalsystems.bankwallet.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable
import kotlinx.parcelize.Parcelize

class SendConfirmationFragment : BaseComposeFragment() {
    val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)

    @Composable
    override fun GetContent() {
        val arguments = requireArguments()
        val sendEntryPointDestId = arguments.getInt(sendEntryPointDestIdKey)

        when (arguments.parcelable<Type>(typeKey)) {
            Type.Bitcoin -> {
                val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)

                SendBitcoinConfirmationScreen(
                    findNavController(),
                    sendBitcoinViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }
            Type.Bep2 -> {
                val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment)

                SendBinanceConfirmationScreen(
                    findNavController(),
                    sendBinanceViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }
            Type.ZCash -> {
                val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment)

                SendZCashConfirmationScreen(
                    findNavController(),
                    sendZCashViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }
            Type.Tron -> {
                val sendTronViewModel by navGraphViewModels<SendTronViewModel>(R.id.sendXFragment)
                SendTronConfirmationScreen(
                    findNavController(),
                    sendTronViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }
            Type.Solana -> {
                val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment)

                SendSolanaConfirmationScreen(
                    findNavController(),
                    sendSolanaViewModel,
                    amountInputModeViewModel,
                    sendEntryPointDestId
                )
            }
            null -> Unit
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, Bep2, ZCash, Solana, Tron
    }

    companion object {
        private const val typeKey = "typeKey"
        private const val sendEntryPointDestIdKey = "sendEntryPointDestIdKey"

        fun prepareParams(type: Type, sendEntryPointDestId: Int) = bundleOf(
            typeKey to type,
            sendEntryPointDestIdKey to sendEntryPointDestId,
        )
    }
}
