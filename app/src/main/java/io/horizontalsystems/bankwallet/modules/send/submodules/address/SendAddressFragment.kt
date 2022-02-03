package io.horizontalsystems.bankwallet.modules.send.submodules.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.address.HSAddressInput
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import io.horizontalsystems.bankwallet.modules.swap.settings.RecipientAddressViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.marketkit.models.PlatformCoin

class SendAddressFragment(
    private val platformCoin: PlatformCoin,
    private val addressModuleDelegate: SendAddressModule.IAddressModuleDelegate,
    private val sendHandler: SendModule.ISendHandler
) : SendSubmoduleFragment() {

    private val viewModel by activityViewModels<RecipientAddressViewModel> {
        SendAddressModule.Factory(platformCoin, sendHandler, addressModuleDelegate, placeholder = getString(R.string.Send_Hint_Address))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            // Dispose the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ComposeAppTheme {
                    HSAddressInput(
                        modifier = Modifier.padding(top = 12.dp),
                        coinType = platformCoin.coinType,
                        coinCode = platformCoin.code,
                        error = viewModel.xxxError
                    ) {
                        viewModel.xxxSetAddress(it)
                    }
                }
            }
        }
    }

    override fun init() {
        // need to init to set lateinit properties
        viewModel
    }
}
