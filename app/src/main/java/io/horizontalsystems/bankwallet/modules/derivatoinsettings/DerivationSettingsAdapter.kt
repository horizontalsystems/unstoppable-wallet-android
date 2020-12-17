package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_derivation_settings.*

class DerivationSettingsAdapter(private val listener: Listener) : RecyclerView.Adapter<DerivationSettingsAdapter.DerivationSettingsItemViewHolder>() {

    interface Listener {
        fun onSettingClick(sectionIndex: Int, settingIndex: Int)
    }

    var items = listOf<DerivationSettingSectionViewItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DerivationSettingsItemViewHolder {
        return DerivationSettingsItemViewHolder(inflate(parent, DerivationSettingsItemViewHolder.layout, false), listener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: DerivationSettingsItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class DerivationSettingsItemViewHolder(override val containerView: View, private val listener: Listener)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(viewItem: DerivationSettingSectionViewItem) {
            bipSectionHeader.text = viewItem.coinName
            bip44.bind(
                    viewItem.items[0].title,
                    viewItem.items[0].subtitle,
                    viewItem.items[0].selected,
                    { listener.onSettingClick(bindingAdapterPosition, 0) },
                    false
            )
            bip49.bind(
                    viewItem.items[1].title,
                    viewItem.items[1].subtitle,
                    viewItem.items[1].selected,
                    { listener.onSettingClick(bindingAdapterPosition, 1) },
                    false
            )
            bip84.bind(
                    viewItem.items[2].title,
                    viewItem.items[2].subtitle,
                    viewItem.items[2].selected,
                    { listener.onSettingClick(bindingAdapterPosition, 2) },
                    true
            )
        }

        companion object {
            const val layout = R.layout.view_holder_derivation_settings
        }
    }
}
