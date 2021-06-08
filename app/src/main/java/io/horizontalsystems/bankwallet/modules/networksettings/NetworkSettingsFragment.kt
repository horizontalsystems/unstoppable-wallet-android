package io.horizontalsystems.bankwallet.modules.networksettings

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_network_settings.*
import kotlinx.android.synthetic.main.view_settings_item_arrow.*

class NetworkSettingsFragment : BaseFragment(R.layout.fragment_network_settings) {

    private val viewModel by viewModels<NetworkSettingsViewModel> {
        NetworkSettingsModule.Factory(requireArguments())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.viewItemsLiveData.observe(viewLifecycleOwner) {
            rvItems.adapter = Adapter(it) {
                viewModel.onSelect(it)
            }
        }

        viewModel.openEvmNetworkLiveEvent.observe(viewLifecycleOwner) {
            findNavController().navigate(
                R.id.networkSettingsFragment_to_evmNetworkFragment,
                EvmNetworkModule.args(it.first, it.second),
                navOptions()
            )
        }
    }

    class Adapter(private val items: List<NetworkSettingsViewModel.ViewItem>, val onSelect: (NetworkSettingsViewModel.ViewItem) -> Unit) : RecyclerView.Adapter<ViewHolder>() {
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder.create(parent, onSelect)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], ListPosition.getListPosition(items.size, position))
        }
    }

    class ViewHolder(override val containerView: View, onSelect: (NetworkSettingsViewModel.ViewItem) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private var item: NetworkSettingsViewModel.ViewItem? = null

        init {
            containerView.setOnSingleClickListener {
                item?.let { onSelect(it) }
            }
        }

        fun bind(item: NetworkSettingsViewModel.ViewItem, listPosition: ListPosition) {
            this.item = item

            settingView.showTitle(item.title)
            settingView.showIcon(ContextCompat.getDrawable(containerView.context, item.iconResId))
            settingView.showValue(item.value)
            settingView.setListPosition(listPosition)
        }

        companion object {
            fun create(parent: ViewGroup, onSelect: (NetworkSettingsViewModel.ViewItem) -> Unit) = ViewHolder(inflate(parent, R.layout.view_settings_item_arrow), onSelect)
        }

    }

}
