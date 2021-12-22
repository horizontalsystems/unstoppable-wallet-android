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
import io.horizontalsystems.bankwallet.databinding.FragmentMarkdownBinding
import io.horizontalsystems.core.findNavController

class MarkdownFragment : BaseFragment(), MarkdownContentAdapter.Listener {

    private lateinit var contentAdapter: MarkdownContentAdapter

    private var _binding: FragmentMarkdownBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkdownBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments?.getBoolean(showAsClosablePopupKey) == true) {
            binding.toolbar.inflateMenu(R.menu.close_menu)
            binding.toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menuClose -> {
                        findNavController().popBackStack()
                        true
                    }
                    else -> false
                }
            }
        } else {
            binding.toolbar.setNavigationIcon(R.drawable.ic_back)
            binding.toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }

        val handleRelativeUrl = arguments?.getBoolean(handleRelativeUrlKey) ?: false
        val markdownUrl = arguments?.getString(markdownUrlKey)
        val gitReleaseUrl = arguments?.getString(gitReleaseNotesUrlKey)
        val viewModel by viewModels<MarkdownViewModel> {
            MarkdownModule.Factory(
                markdownUrl,
                gitReleaseUrl
            )
        }

        contentAdapter = MarkdownContentAdapter(this, handleRelativeUrl)
        binding.rvBlocks.adapter = contentAdapter

        observe(viewModel)
    }

    private fun observe(viewModel: MarkdownViewModel) {
        viewModel.blocks.observe(viewLifecycleOwner, Observer {
            contentAdapter.submitList(it)
        })

        viewModel.statusLiveData.observe(viewLifecycleOwner, Observer {
            binding.error.isVisible = it is LoadStatus.Failed
        })
    }

    //  MarkdownContentAdapter listener

    override fun onClick(url: String) {
        findNavController().navigate(
            R.id.markdownFragment_markdownFragment,
            bundleOf(markdownUrlKey to url),
            navOptions()
        )
    }

    companion object {
        const val markdownUrlKey = "urlKey"
        const val handleRelativeUrlKey = "handleRelativeUrlKey"
        const val gitReleaseNotesUrlKey = "gitReleaseNotesUrlKey"
        const val showAsClosablePopupKey = "showAsClosablePopupKey"
    }
}
