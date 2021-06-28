package io.horizontalsystems.bankwallet.modules.swap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap.*


class SwapMainFragment : BaseFragment() {

    private val vmFactory by lazy { SwapMainModule.Factory(requireArguments()) }
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
                R.id.menuSettings -> {
                    findNavController().navigate(R.id.swapFragment_to_swapSettingsMainFragment)
                    true
                }
                else -> false
            }
        }

        setProviderView(mainViewModel.provider)

        mainViewModel.providerLiveData.observe(viewLifecycleOwner, { provider ->
            setProviderView(provider)
        })
    }

    private fun setProviderView(provider: ISwapProvider) {
        childFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_placeholder, provider.fragment)
                .commitNow()
    }

}
