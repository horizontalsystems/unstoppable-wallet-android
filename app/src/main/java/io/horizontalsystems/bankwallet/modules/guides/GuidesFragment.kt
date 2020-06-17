package io.horizontalsystems.bankwallet.modules.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.modules.guideview.GuideModule
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import kotlinx.android.synthetic.main.fragment_guides.*
import kotlinx.android.synthetic.main.fragment_guides.recyclerTags
import kotlinx.android.synthetic.main.fragment_guides.toolbarSpinner

class GuidesFragment : Fragment(), GuidesAdapter.Listener, FilterAdapter.Listener {

    private val viewModel by viewModels<GuidesViewModel> { GuidesModule.Factory() }
    private val adapter = GuidesAdapter(this)
    private val filterAdapter = FilterAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTags.adapter = filterAdapter
        recyclerGuides.adapter = adapter

        observeLiveData()
    }

    override fun onItemClick(guide: Guide) {
        viewModel.onGuideClick(guide)
    }

    private fun observeLiveData() {
        viewModel.guidesLiveData.observe(viewLifecycleOwner, Observer {
            adapter.items = it
            adapter.notifyDataSetChanged()
        })

        viewModel.filters.observe(viewLifecycleOwner, Observer {
            filterAdapter.setFilters(it.map { FilterAdapter.FilterItem(it) })
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            toolbarSpinner.visibility = if (loading) View.VISIBLE else View.INVISIBLE
        })

        viewModel.openGuide.observe(viewLifecycleOwner, Observer { guide ->
            context?.let {
                GuideModule.start(it, guide)
            }
        })
    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?) {
        item?.filterId?.let {
            recyclerGuides.layoutManager?.scrollToPosition(0)
            viewModel.onSelectFilter(it)
        }
    }
}
