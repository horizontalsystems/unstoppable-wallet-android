package io.horizontalsystems.bankwallet.modules.settings.security.privacy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.blockchainLogo
import io.horizontalsystems.bankwallet.databinding.ViewHolderPrivacySettingsSectionDescriptionBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderPrivacySettingsSectionTitleBinding
import io.horizontalsystems.bankwallet.databinding.ViewHolderSettingWithDropdownBinding
import io.horizontalsystems.bankwallet.entities.title
import io.horizontalsystems.bankwallet.modules.settings.security.privacy.PrivacySettingsModule.IPrivacySettingsViewDelegate

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
            VIEW_TYPE_ITEM -> PrivacySettingsItemViewHolder(
                ViewHolderSettingWithDropdownBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ), delegate
            )
            VIEW_TYPE_TITLE -> TitleViewHolder(
                ViewHolderPrivacySettingsSectionTitleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            VIEW_TYPE_DESCRIPTION -> DescriptionViewHolder(
                ViewHolderPrivacySettingsSectionDescriptionBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
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

    class TitleViewHolder(private val binding: ViewHolderPrivacySettingsSectionTitleBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.titleText.text = text
        }
    }

    class DescriptionViewHolder(private val binding: ViewHolderPrivacySettingsSectionDescriptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.descriptionText.text = text
        }
    }

    class PrivacySettingsItemViewHolder(
        private val binding: ViewHolderSettingWithDropdownBinding,
        private val viewDelegate: IPrivacySettingsViewDelegate
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewItem: PrivacySettingsViewItem) {
            binding.dropdownView.apply {
                showIcon(viewItem.initialSyncSetting.coinType.blockchainLogo)
                showTitle(viewItem.initialSyncSetting.coinType.title)
                showDropdownValue(viewItem.initialSyncSetting.syncMode.title)
                showDropdownIcon(viewItem.enabled)
                setListPosition(viewItem.listPosition)
            }

            binding.dropdownView.isEnabled = viewItem.enabled

            binding.dropdownView.setOnClickListener {
                viewDelegate.onItemTap(viewItem)
            }
        }
    }
}
