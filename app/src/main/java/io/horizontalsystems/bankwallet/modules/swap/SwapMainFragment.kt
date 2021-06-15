package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoFragment.Companion.dexKey
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap.*


class SwapMainFragment : BaseFragment() {

    private val vmFactory by lazy { SwapMainModule.Factory(arguments) }
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                R.id.menuInfo -> {
                    findNavController().navigate(R.id.swapFragment_to_swapInfoFragment, bundleOf(dexKey to mainViewModel.dex), navOptions())
                    true
                }
                else -> false
            }
        }

        initProviderView(mainViewModel.provider)

        mainViewModel.providerLiveData.observe(viewLifecycleOwner, { provider ->
            initProviderView(provider)
        })
    }

    private fun initProviderView(provider: ISwapProvider) {
        childFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_placeholder, provider.fragment)
                .commitNow()
    }

}
