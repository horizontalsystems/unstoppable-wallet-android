package cash.p.terminal.modules.fee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.modules.amount.AmountInputModeViewModel
import cash.p.terminal.modules.send.bitcoin.SendBitcoinViewModel
import io.horizontalsystems.core.findNavController

class FeeSettingsFragment : BaseFragment() {

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
                val viewModel by navGraphViewModels<SendBitcoinViewModel>(R.id.sendXFragment)
                val amountInputModeViewModel by navGraphViewModels<AmountInputModeViewModel>(R.id.sendXFragment)

                FeeSettingsScreen(
                    findNavController(),
                    viewModel,
                    amountInputModeViewModel
                )
            }
        }
    }
}
