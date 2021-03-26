package io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_bottom_notification_menu.*
import kotlinx.android.synthetic.main.view_holder_notification_menu_item.*
import kotlinx.android.synthetic.main.view_holder_notification_menu_section_header.*

class BottomNotificationMenu(
        private val mode: NotificationMenuMode,
        private val coinName: String,
        private val coinType: CoinType
) : BaseBottomSheetDialogFragment(), NotificationMenuItemsAdapter.Listener {

    private val viewModel by viewModels<BottomNotificationsMenuViewModel> { NotificationBottomMenuModule.Factory(coinType, coinName, mode) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_notification_menu)

        setTitle(getString(getTitle(mode)))
        setSubtitle(coinName)
        setHeaderIconDrawable(context?.let { ContextCompat.getDrawable(it, R.drawable.ic_notification_24) })

        val itemsAdapter = NotificationMenuItemsAdapter(this)

        menuItems.adapter = itemsAdapter

        viewModel.menuItemsLiveData.observe(viewLifecycleOwner, Observer { menuItems ->
            itemsAdapter.items = menuItems
            itemsAdapter.notifyDataSetChanged()
        })

    }

    override fun onItemClick(item: NotificationMenuViewItem) {
        viewModel.onOptionClick(item)
    }

    @StringRes
    private fun getTitle(mode: NotificationMenuMode): Int {
        return when (mode) {
            NotificationMenuMode.All -> R.string.Notification_Title
            NotificationMenuMode.Change -> R.string.NotificationBottomMenu_Change24h
            NotificationMenuMode.Trend -> R.string.NotificationBottomMenu_PriceTrendChange
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager, mode: NotificationMenuMode, coinName: String, coinType: CoinType) {
            BottomNotificationMenu(mode, coinName, coinType)
                    .show(fragmentManager, "notification_menu_dialog")
        }
    }
}

class NotificationMenuItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NotificationMenuViewItem)
    }

    var items = listOf<NotificationMenuViewItem>()

    private val sectionHeader = 1
    private val menuItem = 2

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            NotificationViewItemType.BigHeader,
            NotificationViewItemType.SmallHeader -> sectionHeader
            else -> menuItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            menuItem -> NotificationItemViewHolder(
                    inflate(parent, R.layout.view_holder_notification_menu_item, false),
                    onClick = { index ->
                        listener.onItemClick(items[index])
                    })
            sectionHeader -> NotificationBigSectionHeaderViewHolder(
                    inflate(parent, R.layout.view_holder_notification_menu_section_header, false)
            )
            else -> throw Exception("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NotificationItemViewHolder -> holder.bind(items[position])
            is NotificationBigSectionHeaderViewHolder -> holder.bind(items[position])
        }
    }

}

class NotificationItemViewHolder(override val containerView: View, val onClick: (position: Int) -> Unit)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    init {
        containerView.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
    }

    fun bind(item: NotificationMenuViewItem) {
        itemTitle.setText(item.title)
        checkMark.isVisible = item.enabled
    }
}

class NotificationBigSectionHeaderViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: NotificationMenuViewItem) {
        sectionTitle.setText(item.title)
        bigSectionHeader.isVisible = item.type == NotificationViewItemType.BigHeader
    }
}

enum class NotificationMenuMode {
    All, Change, Trend
}
