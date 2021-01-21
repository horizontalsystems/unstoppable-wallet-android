package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.top.MarketTopViewItem
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsAdapter
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsMenuItem
import io.horizontalsystems.views.inflate

class MarketOverviewSectionHeaderAdapter(
        private val viewItemsLiveData: LiveData<List<MarketTopViewItem>>,
        viewLifecycleOwner: LifecycleOwner,
        private val settingsMenuItem: SettingsMenuItem
) : ListAdapter<SettingsMenuItem, MainSettingsAdapter.SettingsViewHolderArrow>(diffCallback) {

    init {
        Transformations
                .distinctUntilChanged(Transformations.map(viewItemsLiveData) { it.isNotEmpty() })
                .observe(viewLifecycleOwner) { showHeader ->
                    val items = if (showHeader) {
                        listOf(settingsMenuItem)
                    } else {
                        listOf()
                    }
                    submitList(items)
                }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainSettingsAdapter.SettingsViewHolderArrow {
        return MainSettingsAdapter.SettingsViewHolderArrow(inflate(parent, R.layout.view_settings_item_arrow))
    }

    override fun onBindViewHolder(holder: MainSettingsAdapter.SettingsViewHolderArrow, position: Int) {
        holder.bind(settingsMenuItem)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<SettingsMenuItem>() {
            override fun areItemsTheSame(oldItem: SettingsMenuItem, newItem: SettingsMenuItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SettingsMenuItem, newItem: SettingsMenuItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}
