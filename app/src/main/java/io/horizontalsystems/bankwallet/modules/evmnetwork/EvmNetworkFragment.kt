package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.basecurrency.RVAdapterSectionHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_evm_network.*
import kotlinx.android.synthetic.main.view_holder_description.*
import kotlinx.android.synthetic.main.view_holder_multiline_lawrence.*

class EvmNetworkFragment : BaseFragment(R.layout.fragment_evm_network) {

    private val viewModel by viewModels<EvmNetworkViewModel> {
        EvmNetworkModule.Factory(
            requireArguments()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = viewModel.title
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.sectionViewItemsLiveData.observe(viewLifecycleOwner) { sectionViewItems ->
            val adapters = mutableListOf<RecyclerView.Adapter<out RecyclerView.ViewHolder>>()
            sectionViewItems.forEach { section ->
                adapters.add(RVAdapterSectionHeader(section.title))
                adapters.add(SectionItemsAdapter(section.viewItems) {
                    viewModel.onSelectViewItem(it)
                })
                section.description?.let {
                    adapters.add(DescriptionAdapter(section.description))
                }
            }

            rvItems.adapter = ConcatAdapter(adapters)
        }

        viewModel.confirmLiveEvent.observe(viewLifecycleOwner) {
            val dialog = TestnetDisclaimerDialog()
            dialog.onConfirm = {
                viewModel.confirmSelection()
            }
            dialog.show(childFragmentManager, "selector_dialog")
        }

        viewModel.finishLiveEvent.observe(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }
}

class SectionItemsAdapter(
    private val items: List<EvmNetworkViewModel.ViewItem>,
    private val onSelect: (EvmNetworkViewModel.ViewItem) -> Unit
) : RecyclerView.Adapter<ViewHolderMultilineLawrence>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolderMultilineLawrence.create(parent, onSelect)

    override fun onBindViewHolder(holder: ViewHolderMultilineLawrence, position: Int) {
        holder.bind(items[position], ListPosition.getListPosition(items.size, position))
    }

    override fun getItemCount() = items.size
}

class ViewHolderMultilineLawrence(
    override val containerView: View,
    onSelect: (EvmNetworkViewModel.ViewItem) -> Unit
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private var item: EvmNetworkViewModel.ViewItem? = null

    init {
        containerView.setOnSingleClickListener {
            item?.let {
                onSelect(it)
            }
        }
    }

    fun bind(item: EvmNetworkViewModel.ViewItem, listPosition: ListPosition) {
        this.item = item

        containerView.setBackgroundResource(listPosition.getBackground())

        title.text = item.name
        subtitle.text = item.url
        checkmarkIcon.isInvisible = !item.selected
    }

    companion object {
        fun create(parent: ViewGroup, onSelect: (EvmNetworkViewModel.ViewItem) -> Unit): ViewHolderMultilineLawrence {
            return ViewHolderMultilineLawrence(inflate(parent, R.layout.view_holder_multiline_lawrence), onSelect)
        }
    }
}

class DescriptionAdapter(
    private val description: String
) : RecyclerView.Adapter<ViewHolderDescription>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolderDescription.create(parent)

    override fun onBindViewHolder(holder: ViewHolderDescription, position: Int) {
        holder.bind(description)
    }

    override fun getItemCount() = 1
}

class ViewHolderDescription(
    override val containerView: View
) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(description: String) {
        text.text = description
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderDescription {
            return ViewHolderDescription(inflate(parent, R.layout.view_holder_description))
        }
    }
}

