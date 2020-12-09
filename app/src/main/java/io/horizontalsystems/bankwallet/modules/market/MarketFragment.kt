package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment(), FilterAdapter.Listener {
    private val filterAdapter = FilterAdapter(this)
    private val viewModel by viewModels<MarketCategoriesViewModel> { MarketModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureSearchMenu(toolbar.menu, R.string.Market_Search)

        recyclerTags.adapter = filterAdapter

        filterAdapter.setFilters(viewModel.categories.map { FilterAdapter.FilterItem(it.name) })

        toolbarSpinner.isVisible = true
    }

    override fun updateFilter(query: String) {

    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {

    }
}
