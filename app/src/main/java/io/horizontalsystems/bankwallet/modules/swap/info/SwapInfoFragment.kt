package io.horizontalsystems.bankwallet.modules.swap.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap_info.*

class SwapInfoFragment : BaseFragment() {

    private val vmFactory by lazy { SwapInfoModule.Factory(requireArguments()) }
    private val viewModel by viewModels<SwapInfoViewModel> { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        toolbar.title = viewModel.title
        description.text = viewModel.description
        headerRelated.text = viewModel.dexRelated
        transactionFeeDescription.text = viewModel.transactionFeeDescription

        btnLinkCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        btnLinkCompose.setContent {
            ComposeAppTheme {
                ButtonSecondaryDefault(
                    modifier = Modifier.padding(top = 44.dp, bottom = 32.dp),
                    title = viewModel.linkText,
                    onClick = {
                        context?.let { ctx ->
                            LinkHelper.openLinkInAppBrowser(ctx, viewModel.dexUrl)
                        }
                    }
                )
            }
        }
    }

}
