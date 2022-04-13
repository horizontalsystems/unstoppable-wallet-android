package io.horizontalsystems.bankwallet.modules.sendx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeModule
import io.horizontalsystems.bankwallet.modules.amount.AmountInputModeViewModel
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendx.bitcoin.SendBitcoinModule
import io.horizontalsystems.bankwallet.modules.sendx.bitcoin.SendBitcoinScreen
import io.horizontalsystems.bankwallet.modules.sendx.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.bankwallet.modules.xrate.XRateModule
import io.horizontalsystems.bankwallet.modules.xrate.XRateViewModel
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinType

class SendFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(SendEvmModule.walletKey)!! }

    private val xRateViewModel by navGraphViewModels<XRateViewModel>(R.id.sendXFragment) { XRateModule.Factory(wallet.coin.uid) }
    private val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) { AmountInputModeModule.Factory() }
    private val sendBitcoinViewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) { SendBitcoinModule.Factory(wallet) }

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
                when (wallet.coinType) {
                    CoinType.Bitcoin -> {
                        SendBitcoinScreen(findNavController(), sendBitcoinViewModel, xRateViewModel, amountInputModeViewModel)
                    }
                    else -> {
                    }
                }

            }
        }
    }
}
