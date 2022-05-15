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
import io.horizontalsystems.bankwallet.databinding.FragmentSwapInfoBinding
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryDefault
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class SwapInfoFragment : BaseFragment() {

    private val vmFactory by lazy { SwapInfoModule.Factory(requireArguments()) }
    private val viewModel by viewModels<SwapInfoViewModel> { vmFactory }

    private var _binding: FragmentSwapInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapInfoBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.title = viewModel.title
        binding.description.text = viewModel.description
        binding.headerRelated.text = viewModel.dexRelated
        binding.transactionFeeDescription.text = viewModel.transactionFeeDescription

        binding.btnLinkCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.btnLinkCompose.setContent {
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
