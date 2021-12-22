package io.horizontalsystems.bankwallet.modules.coin.coinmarkets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentCoinMarketsBinding
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView

class CoinMarketsFragment : BaseFragment(), MarketListHeaderView.Listener {

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by viewModels<CoinMarketsViewModel> {
        CoinMarketsModule.Factory(coinViewModel.fullCoin)
    }

    private var scrollToTopAfterUpdate = false

    private var _binding: FragmentCoinMarketsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCoinMarketsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.marketListHeader.listener = this

        val marketItemsAdapter = CoinMarketItemAdapter()

        binding.recyclerView.adapter = marketItemsAdapter
        binding.recyclerView.itemAnimator = null

        viewModel.tickersLiveData.observe(viewLifecycleOwner, { items ->
            marketItemsAdapter.submitList(items) {
                if (scrollToTopAfterUpdate) {
                    binding.recyclerView.scrollToPosition(0)
                    scrollToTopAfterUpdate = false
                }
            }
        })

        viewModel.topMenuLiveData.observe(viewLifecycleOwner, { (sortMenu, toggleButton) ->
            binding.marketListHeader.setMenu(sortMenu, toggleButton)
        })
    }

    override fun onSortingClick() {
        scrollToTopAfterUpdate = true
        viewModel.onSwitchSortType()
    }

    override fun onToggleButtonClick() {
        viewModel.onSwitchVolumeType()
    }

}
