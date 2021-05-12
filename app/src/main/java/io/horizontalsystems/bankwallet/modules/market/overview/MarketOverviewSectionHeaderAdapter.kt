package io.horizontalsystems.bankwallet.modules.market.overview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import kotlinx.android.extensions.LayoutContainer

class MarketOverviewSectionHeaderAdapter(
        hasItemsLiveData: LiveData<Boolean>,
        viewLifecycleOwner: LifecycleOwner,
        private val settingsHeaderItem: SectionHeaderItem
) : ListAdapter<MarketOverviewSectionHeaderAdapter.SectionHeaderItem, MarketOverviewSectionHeaderAdapter.SectionHeaderViewHolder>(diffCallback) {

    init {
        Transformations
                .distinctUntilChanged(hasItemsLiveData)
                .observe(viewLifecycleOwner) { showHeader ->
                    val items = if (showHeader) {
                        listOf(settingsHeaderItem)
                    } else {
                        listOf()
                    }
                    submitList(items)
                }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionHeaderViewHolder {
        return SectionHeaderViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: SectionHeaderViewHolder, position: Int) {
        holder.bind(settingsHeaderItem)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<SectionHeaderItem>() {
            override fun areItemsTheSame(oldItem: SectionHeaderItem, newItem: SectionHeaderItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SectionHeaderItem, newItem: SectionHeaderItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    class SectionHeaderViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val titleText = containerView.findViewById<TextView>(R.id.titleTextView)
        private val valueText = containerView.findViewById<TextView>(R.id.valueTextView)
        private val icon = containerView.findViewById<ImageView>(R.id.sectionIcon)
        private val arrowIcon = containerView.findViewById<ImageView>(R.id.arrowIcon)

        fun bind(item: SectionHeaderItem) {
            titleText.setText(item.title)
            valueText.text = item.value
            icon.setImageResource(item.icon)
            arrowIcon.isVisible = item.onClick != null
            item.onClick?.let { onClick ->
                containerView.setOnClickListener {
                    onClick()
                }
            }
        }

        companion object {
            fun create(parent: ViewGroup): SectionHeaderViewHolder {
                return SectionHeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_overview_list_section_header, parent, false))
            }
        }
    }

    data class SectionHeaderItem(
            val title: Int,
            val icon: Int,
            var value: String? = null,
            val onClick: (() -> Unit)? = null
    )

}
