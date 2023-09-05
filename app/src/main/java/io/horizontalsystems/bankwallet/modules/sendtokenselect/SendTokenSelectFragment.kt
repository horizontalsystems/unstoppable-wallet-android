package io.horizontalsystems.bankwallet.modules.sendtokenselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.findNavController

class SendTokenSelectFragment : BaseFragment() {

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
                val navController = findNavController()
                TokenSelectScreen(
                    navController = navController,
                    title = stringResource(R.string.Balance_Send),
                    onClickEnabled = { it.sendEnabled },
                    onClickItem = {
                        navController.slideFromRight(
                            R.id.sendXFragment,
                            SendFragment.prepareParams(it.wallet, R.id.sendTokenSelectFragment)
                        )
                    },
                    viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
                    emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
                )
            }
        }
    }
}
