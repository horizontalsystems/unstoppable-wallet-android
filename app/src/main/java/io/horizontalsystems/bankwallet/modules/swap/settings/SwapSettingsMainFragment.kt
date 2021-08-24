package io.horizontalsystems.bankwallet.modules.swap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoModule
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap_settings.*

class SwapSettingsMainFragment : BaseFragment() {
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuInfo -> {
                    SwapInfoModule.start(this, navOptions(), mainViewModel.dex)
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
