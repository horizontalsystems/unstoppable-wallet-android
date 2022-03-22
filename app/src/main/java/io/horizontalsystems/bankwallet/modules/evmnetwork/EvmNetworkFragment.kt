package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.databinding.FragmentEvmNetworkBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderDescriptionBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderMultilineLawrenceBinding
import io.horizontalsystems.bankwallet.modules.basecurrency.RVAdapterSectionHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition

class EvmNetworkFragment : BaseFragment() {

    private val viewModel by viewModels<EvmNetworkViewModel> {
        EvmNetworkModule.Factory(
            requireArguments()
        )
    }

    private var _binding: FragmentEvmNetworkBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvmNetworkBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = viewModel.title
        binding.toolbar.setNavigationOnClickListener {
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

            binding.rvItems.adapter = ConcatAdapter(adapters)
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMultilineLawrence {
        return ViewHolderMultilineLawrence(
            ViewHolderMultilineLawrenceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ), onSelect)
    }

    override fun onBindViewHolder(holder: ViewHolderMultilineLawrence, position: Int) {
        holder.bind(items[position], ListPosition.getListPosition(items.size, position))
    }

    override fun getItemCount() = items.size
}

class ViewHolderMultilineLawrence(
    private val itemBinding: ViewHolderMultilineLawrenceBinding,
    onSelect: (EvmNetworkViewModel.ViewItem) -> Unit
) : RecyclerView.ViewHolder(itemBinding.root) {

    private var item: EvmNetworkViewModel.ViewItem? = null

    init {
        itemBinding.wrapper.setOnSingleClickListener {
            item?.let {
                onSelect(it)
            }
        }
    }

    fun bind(item: EvmNetworkViewModel.ViewItem, listPosition: ListPosition) {
        this.item = item

        itemBinding.wrapper.setBackgroundResource(listPosition.getBackground())

        itemBinding.title.text = item.name
        itemBinding.subtitle.text = item.url
        itemBinding.checkmarkIcon.isInvisible = !item.selected
    }
}

class DescriptionAdapter(
    private val description: String
) : RecyclerView.Adapter<ViewHolderDescription>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderDescription {
        return ViewHolderDescription(
            ViewHolderDescriptionBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolderDescription, position: Int) {
        holder.bind(description)
    }

    override fun getItemCount() = 1
}

class ViewHolderDescription(
    val binding: ViewHolderDescriptionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(description: String) {
        binding.text.text = description
    }
}
