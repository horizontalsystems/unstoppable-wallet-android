package io.horizontalsystems.bankwallet.modules.networksettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentNetworkSettingsBinding
import io.horizontalsystems.bankwallet.databinding.ViewSettingsItemArrowBinding
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition

class NetworkSettingsFragment : BaseFragment() {

    private val viewModel by viewModels<NetworkSettingsViewModel> {
        NetworkSettingsModule.Factory(requireArguments())
    }

    private var _binding: FragmentNetworkSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner) {
            binding.rvItems.adapter = Adapter(it) {
                viewModel.onSelect(it)
            }
        }

        viewModel.openEvmNetworkLiveEvent.observe(viewLifecycleOwner) {
            findNavController().slideFromRight(
                R.id.networkSettingsFragment_to_evmNetworkFragment,
                EvmNetworkModule.args(it.first, it.second)
            )
        }
    }

    class Adapter(
        private val items: List<NetworkSettingsViewModel.ViewItem>,
        val onSelect: (NetworkSettingsViewModel.ViewItem) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                ViewSettingsItemArrowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), onSelect)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], ListPosition.getListPosition(items.size, position))
        }
    }

    class ViewHolder(
        private val binding: ViewSettingsItemArrowBinding,
        onSelect: (NetworkSettingsViewModel.ViewItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var item: NetworkSettingsViewModel.ViewItem? = null

        init {
            binding.settingView.setOnSingleClickListener {
                item?.let { onSelect(it) }
            }
        }

        fun bind(item: NetworkSettingsViewModel.ViewItem, listPosition: ListPosition) {
            this.item = item

            binding.settingView.showTitle(item.title)
            binding.settingView.showIcon(
                ContextCompat.getDrawable(
                    binding.settingView.context,
                    item.iconResId
                )
            )
            binding.settingView.showValue(item.value)
            binding.settingView.setListPosition(listPosition)
        }

    }

}
