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
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmModule
import io.horizontalsystems.core.findNavController

class SendXFragment : BaseFragment() {

    private val wallet by lazy { requireArguments().getParcelable<Wallet>(SendEvmModule.walletKey)!! }

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
                val viewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment) { SendModule.Factory(wallet) }
                val xRateViewModel by navGraphViewModels<XRateViewModel>(R.id.sendXFragment) { XRateModule.Factory(wallet.coin.uid) }
                val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment) { AmountInputModeModule.Factory() }

                SendScreen(findNavController(), viewModel, xRateViewModel, amountInputModeViewModel)
            }
        }
    }
}
