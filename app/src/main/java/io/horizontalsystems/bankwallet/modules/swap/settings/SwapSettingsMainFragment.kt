package io.horizontalsystems.bankwallet.modules.swap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentSwapSettingsBinding
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoModule
import io.horizontalsystems.core.findNavController

class SwapSettingsMainFragment : BaseFragment() {
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)

    private var _binding: FragmentSwapSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwapSettingsBinding.inflate(inflater, container, false)
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
                R.id.menuInfo -> {
                    findNavController().slideFromRight(
                        R.id.swapSettingsMainFragment_to_swapInfoFragment,
                        SwapInfoModule.prepareParams(mainViewModel.dex)
                    )
                    true
                }
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        childFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_placeholder, mainViewModel.provider.settingsFragment)
            .commitNow()
    }
}
