package io.horizontalsystems.bankwallet.modules.receivemain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class DerivationSelectFragment : BaseFragment() {

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
                val coinUid = arguments?.getString("coinUid")

                if (coinUid == null) {
                    HudHelper.showErrorMessage(LocalView.current, R.string.Error_ParameterNotSet)
                    navController.popBackStack()
                } else {
                    val viewModel = viewModel<DerivationSelectViewModel>(
                        factory = DerivationSelectViewModel.Factory(coinUid)
                    )
                    AddressFormatSelectScreen(
                        navController,
                        viewModel.items,
                        stringResource(R.string.Balance_Receive_AddressFormat_RecommendedDerivation)
                    )
                }
            }
        }
    }

    companion object {
        fun prepareParams(coinUid: String) = bundleOf("coinUid" to coinUid)
    }
}