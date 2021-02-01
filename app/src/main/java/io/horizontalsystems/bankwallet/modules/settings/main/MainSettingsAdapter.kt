package io.horizontalsystems.bankwallet.modules.settings.main

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.SettingsView
import io.horizontalsystems.views.SettingsViewSwitch
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer

class MainSettingsAdapter(private val items: List<SettingsViewItem?>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val viewTypeArrow = 0
    private val viewTypeSwitch = 1
    private val viewTypeSpace = 2
    private val viewTypeBottom = 3

    fun notifyChanged(element: SettingsViewItem) {
        val index = items.indexOf(element)
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            viewTypeSpace -> SettingsViewHolderSpace(inflate(parent, R.layout.view_settings_item_space))
            viewTypeBottom -> SettingsViewHolderBottom(inflate(parent, R.layout.view_settings_item_bottom))
            viewTypeSwitch -> SettingsViewHolderSwitch(inflate(parent, R.layout.view_holder_setting_item_with_switch))
            else -> SettingsViewHolderArrow(inflate(parent, R.layout.view_settings_item_arrow))
        }
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SettingsMenuItem -> viewTypeArrow
        is SettingsMenuSwitch -> viewTypeSwitch
        is SettingsMenuBottom -> viewTypeBottom
        else -> viewTypeSpace
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is SettingsViewHolderBottom -> {
                if (item is SettingsMenuBottom) {
                    holder.bind(item)
                }
            }
            is SettingsViewHolderSwitch -> {
                if (item is SettingsMenuSwitch) {
                    holder.bind(item)
                }
            }
            is SettingsViewHolderArrow -> {
                if (item is SettingsMenuItem) {
                    holder.bind(item)
                }
            }
        }
    }

    class SettingsViewHolderSpace(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer

    class SettingsViewHolderBottom(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {
        private val companyLogo = containerView.findViewById<ImageView>(R.id.companyLogo)
        private val appName = containerView.findViewById<TextView>(R.id.appName)

        fun bind(item: SettingsMenuBottom) {
            appName.text  = item.appName
            companyLogo.setOnClickListener {
                item.onClick()
            }
        }
    }

    class SettingsViewHolderSwitch(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val switchSettingView = containerView.findViewById<SettingsViewSwitch>(R.id.switchSettingView)

        fun bind(item: SettingsMenuSwitch) {
            switchSettingView.apply {
                showTitle(containerView.context.getString(item.title))
                setListPosition(item.listPosition)
                setChecked(item.isChecked)
                showIcon(LayoutHelper.d(item.icon, containerView.context))

                setOnCheckedChangeListenerSingle { checked ->
                    item.onClick(checked)
                }
            }
        }
    }

    class SettingsViewHolderArrow(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val settingView = containerView.findViewById<SettingsView>(R.id.settingView)

        fun bind(item: SettingsMenuItem) {
            settingView.apply {
                showTitle(containerView.context.getString(item.title))
                setListPosition(item.listPosition)
                showAttention(item.attention)
                showValue(item.value)
                showIcon(LayoutHelper.d(item.icon, containerView.context))

                setOnClickListener {
                    item.onClick()
                }
            }
        }
    }
}

open class SettingsViewItem

class SettingsMenuItem(
        val title: Int,
        val icon: Int,
        var value: String? = null,
        var attention: Boolean = false,
        val listPosition: ListPosition,
        val onClick: () -> Unit) : SettingsViewItem()

class SettingsMenuSwitch(
        val title: Int,
        val icon: Int,
        var isChecked: Boolean = false,
        val listPosition: ListPosition,
        val onClick: (isChecked: Boolean) -> Unit) : SettingsViewItem()

class SettingsMenuBottom(
        var appName: String? = null,
        val onClick: () -> Unit) : SettingsViewItem()
