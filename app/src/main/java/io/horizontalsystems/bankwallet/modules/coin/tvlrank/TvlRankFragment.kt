package io.horizontalsystems.bankwallet.modules.coin.tvlrank

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinInfoErrorAdapter
import io.horizontalsystems.bankwallet.modules.coin.PoweredByAdapter
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.TvlRankListHeaderView
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_tvl_rank.*

class TvlRankFragment : BaseFragment(), TvlRankListHeaderView.Listener {

    private val viewModel by viewModels<TvlRankViewModel> { TvlRankModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tvl_rank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = getString(R.string.TvlRank_Title)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        listHeader.listener = this

        val adapter = TvlRankItemAdapter()
        val loadingAdapter = TvlRankLoadingAdapter(
            viewModel.loadingLiveData,
            viewLifecycleOwner
        )
        val errorAdapter = CoinInfoErrorAdapter(viewModel.coinInfoErrorLiveData, viewLifecycleOwner)
        val poweredByAdapter = PoweredByAdapter(viewModel.showPoweredByLiveData, viewLifecycleOwner, getString(R.string.Market_PoweredByDefiLlamaApi))

        recyclerView.adapter = ConcatAdapter(loadingAdapter, errorAdapter, adapter, poweredByAdapter)
        recyclerView.itemAnimator = null

        viewModel.coinList.observe(viewLifecycleOwner, {
            adapter.submitList(it){
                recyclerView.scrollToPosition(0)
            }
        })

        viewModel.sortDescLiveData.observe(viewLifecycleOwner, { sortDesc ->
            listHeader.setSortDescending(sortDesc)
        })

        viewModel.filterLiveData.observe(viewLifecycleOwner, { filterField ->
            listHeader.setLeftField(filterField.titleResId)
        })

        viewModel.showFilterSelectDialogLiveData.observe(viewLifecycleOwner, { filters ->
            SelectorDialog
                .newInstance(filters, getString(R.string.TvlRank_Filters_PopupTitle)) { position ->
                    viewModel.onFilterSelect(position)
                }
                .show(childFragmentManager, "filter_field_selector")
        })
    }

    override fun onFilterClick() {
        viewModel.onFilterMenuClick()
    }

    override fun onChangeSortingClick() {
        viewModel.onChangeSorting()
    }

}
