package io.horizontalsystems.bankwallet.modules.settings.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.databinding.FragmentFaqListBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderFaqItemBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderFaqSectionBinding
import io.horizontalsystems.bankwallet.entities.Faq
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.settings.guides.ErrorAdapter
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition

class FaqListFragment : BaseFragment(), FaqListAdapter.Listener {

    private val viewModel by viewModels<FaqViewModel> { FaqModule.Factory() }
    private val adapter = FaqListAdapter(this)
    private val errorAdapter = ErrorAdapter()

    private var _binding: FragmentFaqListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.faqListRecyclerview.adapter = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.faqListRecyclerview.adapter = ConcatAdapter(errorAdapter, adapter)

        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.faqItemList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            binding.toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    override fun onItemClicked(faqItem: FaqItem) {
        val arguments = bundleOf(MarkdownFragment.markdownUrlKey to faqItem.faq.markdown)
        findNavController().navigate(R.id.faqFragment_to_markdownFragment, arguments, navOptions())
    }
}

open class FaqData
data class FaqSection(val title: String) : FaqData()
data class FaqItem(val faq: Faq, var listPosition: ListPosition) : FaqData()

class FaqListAdapter(private val listener: Listener) :
    ListAdapter<FaqData, RecyclerView.ViewHolder>(faqDiff) {

    interface Listener {
        fun onItemClicked(faqItem: FaqItem)
    }

    private val viewTypeSection = 0
    private val viewTypeFaq = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            viewTypeSection -> {
                ViewHolderSection(
                    ViewHolderFaqSectionBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
            else -> {
                ViewHolderFaq(
                    ViewHolderFaqItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ), listener)
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        if (holder is ViewHolderSection && item is FaqSection) {
            holder.bind(item)
        }
        if (holder is ViewHolderFaq && item is FaqItem) {
            holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FaqSection -> viewTypeSection
        else -> viewTypeFaq
    }

    companion object {
        private val faqDiff = object : DiffUtil.ItemCallback<FaqData>() {
            override fun areItemsTheSame(oldItem: FaqData, newItem: FaqData): Boolean {
                return oldItem.equals(newItem)
            }

            override fun areContentsTheSame(oldItem: FaqData, newItem: FaqData): Boolean {
                return oldItem.equals(newItem)
            }
        }
    }
}

class ViewHolderSection(private val binding: ViewHolderFaqSectionBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(item: FaqSection) {
        binding.faqHeadText.text = item.title
    }
}

class ViewHolderFaq(
    private val binding: ViewHolderFaqItemBinding,
    listener: FaqListAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {
    private var faqItem: FaqItem? = null

    init {
        binding.wrapper.setOnClickListener {
            faqItem?.let {
                listener.onItemClicked(it)
            }
        }
    }

    fun bind(item: FaqItem) {
        faqItem = item
        binding.faqTitleText.text = item.faq.title
        binding.wrapper.setBackgroundResource(item.listPosition.getBackground())
    }
}
