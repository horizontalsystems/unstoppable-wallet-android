package io.horizontalsystems.bankwallet.modules.settings.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_guides.*
import kotlinx.android.synthetic.main.fragment_guides.tabsCompose
import kotlinx.android.synthetic.main.fragment_guides.toolbar

class GuidesFragment : BaseFragment(), GuidesAdapter.Listener {

    private val viewModel by viewModels<GuidesViewModel> { GuidesModule.Factory() }
    private val errorAdapter = ErrorAdapter()
    private val guidesAdapter = GuidesAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_guides, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        recyclerGuides.adapter = ConcatAdapter(errorAdapter, guidesAdapter)

        observeLiveData()

        tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerGuides.adapter = null
    }

    override fun onItemClick(guide: Guide) {
        val arguments = bundleOf(
            MarkdownFragment.markdownUrlKey to guide.fileUrl,
            MarkdownFragment.handleRelativeUrlKey to true
        )
        findNavController().navigate(
            R.id.academyFragment_to_markdownFragment,
            arguments,
            navOptions()
        )
    }

    private fun observeLiveData() {
        viewModel.guides.observe(viewLifecycleOwner, Observer {
            guidesAdapter.items = it
            guidesAdapter.notifyDataSetChanged()
        })

        viewModel.selectedCategory.observe(viewLifecycleOwner, Observer { selectedCategory ->
            setTabs(selectedCategory)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    private fun setTabs(selectedCategory: GuideCategory) {
        val tabItems = viewModel.categories.map { TabItem(it.category, it == selectedCategory, it) }
        tabsCompose.setContent {
            ComposeAppTheme {
                ScrollableTabs(tabItems) { tab -> viewModel.onSelectCategory(tab) }
            }
        }
    }

}
