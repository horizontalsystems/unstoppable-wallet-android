package io.horizontalsystems.bankwallet.modules.markdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_markdown.*

class MarkdownFragment : BaseFragment(), MarkdownContentAdapter.Listener {

    private val contentAdapter = MarkdownContentAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_markdown, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = ""
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        rvBlocks.adapter = contentAdapter

        val markdownUrl = arguments?.getString(markdownUrlKey)
        val viewModel by viewModels<MarkdownViewModel> { MarkdownModule.Factory(markdownUrl) }

        observe(viewModel)
    }

    private fun observe(viewModel: MarkdownViewModel) {
        viewModel.blocks.observe(viewLifecycleOwner, Observer {
            contentAdapter.submitList(it)
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, Observer {
            error.isVisible = it is LoadStatus.Failed
        })
    }

    //  MarkdownContentAdapter listener

    override fun onClick(url: String) {
        findNavController().navigate(R.id.markdownFragment_markdownFragment, bundleOf(markdownUrlKey to url), navOptions())
    }

    companion object {
        const val markdownUrlKey = "urlKey"
    }
}
