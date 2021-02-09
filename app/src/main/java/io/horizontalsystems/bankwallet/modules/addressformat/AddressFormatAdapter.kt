package io.horizontalsystems.bankwallet.modules.addressformat

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.SettingItemWithCheckmark
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_derivation_settings.*

class AddressFormatAdapter(private val listener: Listener) : RecyclerView.Adapter<AddressFormatAdapter.AddressFormatItemViewHolder>() {

    interface Listener {
        fun onSettingClick(sectionIndex: Int, settingIndex: Int)
    }

    var items = listOf<AddressFormatModule.SectionItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressFormatItemViewHolder {
        return AddressFormatItemViewHolder(inflate(parent, AddressFormatItemViewHolder.layout, false), listener)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: AddressFormatItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class AddressFormatItemViewHolder(override val containerView: View, private val listener: Listener)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val optionViewIds = listOf(R.id.option1, R.id.option2, R.id.option3)

        fun bind(viewItem: AddressFormatModule.SectionItem) {
            bipSectionHeader.text = viewItem.coinTypeName
            option3.isVisible = viewItem.viewItems.size == 3

            viewItem.viewItems.forEachIndexed { index, item ->
                containerView.findViewById<SettingItemWithCheckmark>(optionViewIds[index])?.apply {
                    bind(
                            item.title,
                            item.subtitle,
                            item.selected,
                            { listener.onSettingClick(bindingAdapterPosition, index) },
                            item.listPosition
                    )
                }
            }
        }

        companion object {
            const val layout = R.layout.view_holder_derivation_settings
        }
    }
}
