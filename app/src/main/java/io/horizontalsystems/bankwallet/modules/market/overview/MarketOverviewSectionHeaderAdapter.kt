package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsAdapter
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsMenuItem
import io.horizontalsystems.views.inflate

class MarketOverviewSectionHeaderAdapter(val settingsMenuItem: SettingsMenuItem) : RecyclerView.Adapter<MainSettingsAdapter.SettingsViewHolderArrow>() {

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainSettingsAdapter.SettingsViewHolderArrow {
        return MainSettingsAdapter.SettingsViewHolderArrow(inflate(parent, R.layout.view_settings_item_arrow))
    }

    override fun onBindViewHolder(holder: MainSettingsAdapter.SettingsViewHolderArrow, position: Int) {
        holder.bind(settingsMenuItem)
    }

}
