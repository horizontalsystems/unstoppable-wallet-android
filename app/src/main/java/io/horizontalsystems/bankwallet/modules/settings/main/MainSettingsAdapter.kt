package io.horizontalsystems.bankwallet.modules.settings.main

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
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
            viewTypeSwitch -> SettingsViewHolderSwitch(inflate(parent, R.layout.view_settings_item_switch))
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

        private val settingsTitle = containerView.findViewById<TextView>(R.id.settingsTitle)
        private val settingsIcon = containerView.findViewById<ImageView>(R.id.settingsIcon)
        private val switchSettings = containerView.findViewById<SwitchCompat>(R.id.switchSettings)
        private var isTouched = false

        fun bind(item: SettingsMenuSwitch) {
            settingsIcon.setImageResource(item.icon)
            settingsTitle.setText(item.title)

            switchSettings.isChecked = item.isChecked
            switchSettings.setOnTouchListener { _, _ ->
                isTouched = true
                false
            }

            switchSettings.setOnCheckedChangeListener { _, isChecked ->
                if (isTouched) {
                    isTouched = false
                    item.onClick(isChecked)
                }
            }

            containerView.setOnClickListener {
                switchSettings.toggle()
                item.onClick(switchSettings.isChecked)
            }
        }
    }

    class SettingsViewHolderArrow(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        private val settingsTitle = containerView.findViewById<TextView>(R.id.settingsTitle)
        private val settingsValue = containerView.findViewById<TextView>(R.id.settingsValueRight)
        private val settingsIcon = containerView.findViewById<ImageView>(R.id.settingsIcon)
        private val bottomBorder = containerView.findViewById<View>(R.id.bottomBorder)
        private val arrowIcon = containerView.findViewById<ImageView>(R.id.arrowIcon)
        private val attentionIcon = containerView.findViewById<ImageView>(R.id.attentionIcon)

        fun bind(item: SettingsMenuItem) {
            settingsTitle.setText(item.title)

            bottomBorder.isVisible = item.isLast
            arrowIcon.isVisible = true
            attentionIcon.isVisible = item.attention

            settingsValue.text = item.value
            settingsValue.isVisible = item.value != null

            settingsIcon.setImageResource(item.icon)
            containerView.setOnClickListener {
                item.onClick()
            }
        }
    }
}

open class SettingsViewItem

class SettingsMenuItem(
        val title: Int,
        val icon: Int,
        val isLast: Boolean = false,
        var value: String? = null,
        var attention: Boolean = false,
        val onClick: () -> Unit) : SettingsViewItem()

class SettingsMenuSwitch(
        val title: Int,
        val icon: Int,
        var isChecked: Boolean = false,
        val onClick: (isChecked: Boolean) -> Unit) : SettingsViewItem()

class SettingsMenuBottom(
        var appName: String? = null,
        val onClick: () -> Unit) : SettingsViewItem()
