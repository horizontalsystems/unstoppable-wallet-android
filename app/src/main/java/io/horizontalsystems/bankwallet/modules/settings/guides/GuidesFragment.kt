package io.horizontalsystems.bankwallet.modules.settings.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.FilterAdapter
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_guides.*

class GuidesFragment : BaseFragment(), GuidesAdapter.Listener, FilterAdapter.Listener {

    private val viewModel by viewModels<GuidesViewModel> { GuidesModule.Factory() }
    private val errorAdapter = ErrorAdapter()
    private val guidesAdapter = GuidesAdapter(this)
    private val filterAdapter = FilterAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        recyclerTags.adapter = filterAdapter
        recyclerGuides.adapter = ConcatAdapter(errorAdapter, guidesAdapter)

        observeLiveData()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        recyclerTags.adapter = null
        recyclerGuides.adapter = null
    }

    override fun onItemClick(guide: Guide) {
        val arguments = bundleOf(
                MarkdownFragment.markdownUrlKey to guide.fileUrl,
                MarkdownFragment.handleRelativeUrlKey to true
        )
        findNavController().navigate(R.id.academyFragment_to_markdownFragment, arguments, navOptions())
    }

    private fun observeLiveData() {
        viewModel.guides.observe(viewLifecycleOwner, Observer {
            guidesAdapter.items = it
            guidesAdapter.notifyDataSetChanged()
        })

        viewModel.filters.observe(viewLifecycleOwner, Observer {
            val selectedFilterItem = viewModel.selectedFilter?.let{ selected -> FilterAdapter.FilterItem(selected) }
            filterAdapter.setFilters(it.map { filter -> FilterAdapter.FilterItem(filter) }, selectedFilterItem)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {
        item?.filterId?.let {
            recyclerGuides.layoutManager?.scrollToPosition(0)
            viewModel.onSelectFilter(it)

            val leftOffset = recyclerTags.width / 2 - itemWidth / 2
            (recyclerTags.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(itemPosition, leftOffset)
        }
    }
}
