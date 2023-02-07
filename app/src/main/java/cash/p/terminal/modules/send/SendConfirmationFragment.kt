package cash.p.terminal.modules.send

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.binance.SendBinanceConfirmationScreen
import cash.p.terminal.modules.send.binance.SendBinanceViewModel
import cash.p.terminal.modules.send.bitcoin.SendBitcoinConfirmationScreen
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import cash.p.terminal.modules.send.solana.SendSolanaConfirmationScreen
import cash.p.terminal.modules.send.solana.SendSolanaViewModel
import cash.p.terminal.modules.send.zcash.SendZCashConfirmationScreen
import cash.p.terminal.modules.send.zcash.SendZCashViewModel
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class SendConfirmationFragment : BaseFragment() {
    val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                when (requireArguments().getParcelable<Type>(typeKey)) {
                    Type.Bitcoin -> {
                        val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)

                        SendBitcoinConfirmationScreen(
                            findNavController(),
                            sendBitcoinViewModel,
                            amountInputModeViewModel
                        )
                    }
                    Type.Bep2 -> {
                        val sendBinanceViewModel by navGraphViewModels<SendBinanceViewModel>(R.id.sendXFragment)

                        SendBinanceConfirmationScreen(
                            findNavController(),
                            sendBinanceViewModel,
                            amountInputModeViewModel
                        )
                    }
                    Type.ZCash -> {
                        val sendZCashViewModel by navGraphViewModels<SendZCashViewModel>(R.id.sendXFragment)

                        SendZCashConfirmationScreen(
                            findNavController(),
                            sendZCashViewModel,
                            amountInputModeViewModel
                        )
                    }
                    Type.Solana -> {
                        val sendSolanaViewModel by navGraphViewModels<SendSolanaViewModel>(R.id.sendXFragment)

                        SendSolanaConfirmationScreen(
                                findNavController(),
                                sendSolanaViewModel,
                                amountInputModeViewModel
                        )
                    }
                    null -> Unit
                }
            }
        }
    }

    @Parcelize
    enum class Type : Parcelable {
        Bitcoin, Bep2, ZCash, Solana
    }

    companion object {
        private const val typeKey = "typeKey"

        fun prepareParams(type: Type) = bundleOf(
            typeKey to type
        )
    }
}
