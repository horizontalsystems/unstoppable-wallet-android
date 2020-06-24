package io.horizontalsystems.bankwallet.ui.extensions

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_bottom_notification_menu.*
import kotlinx.android.synthetic.main.view_holder_notification_menu_item.*
import kotlinx.android.synthetic.main.view_holder_notification_menu_section_header.*

class BottomNotificationMenu(
        private val mode: NotificationMenuMode,
        private val coinTitle: String,
        private val coinCode: String
) : BaseBottomSheetDialogFragment(), NotificationMenuItemsAdapter.Listener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setContentView(R.layout.fragment_bottom_notification_menu)

        setTitle(getString(getTitleRes(mode)))
        setSubtitle(coinTitle)
        setHeaderIconDrawable(context?.let { ContextCompat.getDrawable(it, R.drawable.ic_notification_24) })

        val items = listOf(
                NotificationMenuItem.ChangeHeader,
                NotificationMenuItem.ChangeOff(true),
                NotificationMenuItem.Change2(false),
                NotificationMenuItem.Change5(false),
                NotificationMenuItem.Change10(false),
                NotificationMenuItem.TrendHeader,
                NotificationMenuItem.TrendOff(true)
        )

        val itemsAdapter = NotificationMenuItemsAdapter(items, this)

        menuItems.adapter = itemsAdapter

    }

    override fun onItemClick(item: NotificationMenuItem) {
        TODO("Not yet implemented")
    }

    @StringRes
    private fun getTitleRes(mode: NotificationMenuMode): Int{
        return when(mode){
            NotificationMenuMode.All -> R.string.Notification_Title
            NotificationMenuMode.Change -> R.string.NotificationBottomMenu_Change24h
            NotificationMenuMode.Trend -> R.string.NotificationBottomMenu_PriceTrendChange
        }
    }

    companion object {
        fun show(fragmentManager: FragmentManager, mode: NotificationMenuMode, coinTitle: String, coinCode: String) {
            BottomNotificationMenu(mode, coinTitle, coinCode)
                    .show(fragmentManager, "notification_menu_dialog")
        }
    }
}

class NotificationMenuItemsAdapter(
        private val items: List<NotificationMenuItem>,
        private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onItemClick(item: NotificationMenuItem)
    }

    private val sectionHeader = 1
    private val menuItem = 2

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            NotificationMenuItem.ChangeHeader,
            NotificationMenuItem.TrendHeader -> sectionHeader
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

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
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

    fun bind(item: NotificationMenuItem) {
        itemTitle.setText(item.getTitle())
        checkMark.isVisible = item.isEnabled()
    }
}

class NotificationBigSectionHeaderViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: NotificationMenuItem) {
        sectionTitle.setText(item.getTitle())
        bigSectionHeader.isVisible = item is NotificationMenuItem.TrendHeader
    }
}

enum class NotificationMenuMode {
    All, Change, Trend
}

sealed class NotificationMenuItem {
    object ChangeHeader : NotificationMenuItem()
    object TrendHeader : NotificationMenuItem()
    class ChangeOff(var enabled: Boolean) : NotificationMenuItem()
    class Change2(var enabled: Boolean) : NotificationMenuItem()
    class Change5(var enabled: Boolean) : NotificationMenuItem()
    class Change10(var enabled: Boolean) : NotificationMenuItem()
    class TrendOff(var enabled: Boolean) : NotificationMenuItem()
    class TrendShortTerm(var enabled: Boolean) : NotificationMenuItem()
    class TrendLongTerm(var enabled: Boolean) : NotificationMenuItem()

    @StringRes
    fun getTitle(): Int {
        return when (this) {
            ChangeHeader -> R.string.NotificationBottomMenu_Change24h
            TrendHeader -> R.string.NotificationBottomMenu_PriceTrendChange
            is ChangeOff, is TrendOff -> R.string.NotificationBottomMenu_Off
            is Change2 -> R.string.NotificationBottomMenu_2
            is Change5 -> R.string.NotificationBottomMenu_5
            is Change10 -> R.string.NotificationBottomMenu_10
            is TrendShortTerm -> R.string.NotificationBottomMenu_ShortTerm
            is TrendLongTerm -> R.string.NotificationBottomMenu_LongTerm
        }
    }

    fun isEnabled(): Boolean {
        return when (this) {
            is ChangeOff -> enabled
            is TrendOff -> enabled
            is Change2 -> enabled
            is Change5 -> enabled
            is Change10 -> enabled
            is TrendShortTerm -> enabled
            is TrendLongTerm -> enabled
            ChangeHeader, TrendHeader -> throw Exception("Invalid type")
        }
    }
}
