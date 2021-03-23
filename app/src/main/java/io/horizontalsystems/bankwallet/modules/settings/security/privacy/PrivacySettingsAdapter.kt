package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule.IPrivacySettingsViewDelegate
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.SettingsViewDropdown
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

class PrivacySettingsAdapter(
        private val delegate: IPrivacySettingsViewDelegate,
        private val title: String,
        private val description: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ITEM = 1
    private val VIEW_TYPE_TITLE = 2
    private val VIEW_TYPE_DESCRIPTION = 3

    var items = listOf<PrivacySettingsViewItem>()

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_TITLE
            items.size + 1 -> VIEW_TYPE_DESCRIPTION
            else -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ITEM -> PrivacySettingsItemViewHolder(inflate(parent, R.layout.view_holder_setting_with_dropdown, false), delegate)
            VIEW_TYPE_TITLE -> TitleViewHolder.create(parent)
            VIEW_TYPE_DESCRIPTION -> DescriptionViewHolder.create(parent)
            else -> throw IllegalStateException("No such view type")
        }
    }

    override fun getItemCount(): Int {
        if (items.isEmpty()) {
            return 0
        }
        return items.size + 2
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TitleViewHolder -> holder.bind(title)
            is DescriptionViewHolder -> holder.bind(description)
            is PrivacySettingsItemViewHolder -> holder.bind(items[position - 1])
        }
    }

    class TitleViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(text: String) {
            containerView.findViewById<TextView>(R.id.titleText)?.text = text
        }

        companion object {
            const val layout = R.layout.view_holder_privacy_settings_section_title

            fun create(parent: ViewGroup) = TitleViewHolder(inflate(parent, layout, false))
        }

    }

    class DescriptionViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        fun bind(text: String) {
            containerView.findViewById<TextView>(R.id.descriptionText)?.text = text
        }

        companion object {
            const val layout = R.layout.view_holder_privacy_settings_section_description

            fun create(parent: ViewGroup) = DescriptionViewHolder(inflate(parent, layout, false))
        }

    }

    class PrivacySettingsItemViewHolder(override val containerView: View, private val viewDelegate: IPrivacySettingsViewDelegate)
        : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val dropdownView = containerView.findViewById<SettingsViewDropdown>(R.id.dropdownView)

        fun bind(viewItem: PrivacySettingsViewItem) {
            dropdownView.apply {
                showIcon(AppLayoutHelper.getCoinDrawable(containerView.context, viewItem.coin.type))
                showTitle(viewItem.title)
                showDropdownValue(viewItem.settingType.selectedTitle)
                showDropdownIcon(viewItem.enabled)
                setListPosition(viewItem.listPosition)
            }

            containerView.isEnabled = viewItem.enabled

            containerView.setOnClickListener {
                viewDelegate.onItemTap(viewItem.settingType, bindingAdapterPosition - 1)
            }
        }
    }
}

