package io.horizontalsystems.bankwallet.modules.settings.faq

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
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
import io.horizontalsystems.bankwallet.entities.Faq
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.settings.guides.ErrorAdapter
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import kotlinx.android.extensions.LayoutContainer

class FaqListFragment: BaseFragment(), FaqListAdapter.Listener {

    private val viewModel by viewModels<FaqViewModel> { FaqModule.Factory() }
    private val adapter = FaqListAdapter(this)
    private val errorAdapter = ErrorAdapter()
    private lateinit var faqListRecyclerview: RecyclerView
    private lateinit var toolbarSpinner: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_faq_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        faqListRecyclerview = view.findViewById(R.id.faqListRecyclerview)
        toolbarSpinner = view.findViewById(R.id.toolbarSpinner)

        view.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        faqListRecyclerview.adapter = ConcatAdapter(errorAdapter, adapter)

        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.faqItemList.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()

        faqListRecyclerview.adapter = null
    }

    override fun onItemClicked(faqItem: FaqItem) {
        val arguments = bundleOf(MarkdownFragment.markdownUrlKey to faqItem.faq.markdown)
        findNavController().navigate(R.id.faqFragment_to_markdownFragment, arguments, navOptions())
    }
}

data class FaqItem(val faq: Faq, var listPosition: ListPosition)

class FaqListAdapter(private val listener: Listener) : ListAdapter<FaqItem, ViewHolderFaq>(faqDiff) {

    interface Listener {
        fun onItemClicked(faqItem: FaqItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFaq {
        return ViewHolderFaq(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_faq_item, parent, false), listener)
    }

    override fun onBindViewHolder(holder: ViewHolderFaq, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val faqDiff = object: DiffUtil.ItemCallback<FaqItem>() {
            override fun areItemsTheSame(oldItem: FaqItem, newItem: FaqItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: FaqItem, newItem: FaqItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

class ViewHolderFaq(override val containerView: View, listener: FaqListAdapter.Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private var faqItem: FaqItem? = null
    private val faqTitleText = containerView.findViewById<TextView>(R.id.faqTitleText)

    init {
        containerView.setOnClickListener {
            faqItem?.let {
                listener.onItemClicked(it)
            }
        }
    }

    fun bind(item: FaqItem) {
        this.faqItem = item
        faqTitleText.text = item.faq.title
        containerView.setBackgroundResource(item.listPosition.getBackground())
    }
}
