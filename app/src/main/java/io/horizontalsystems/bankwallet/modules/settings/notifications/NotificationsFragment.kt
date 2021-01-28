package io.horizontalsystems.bankwallet.modules.settings.notifications

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.SettingsViewDropdown
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_notifications.*
import kotlinx.android.synthetic.main.view_holder_notification_coin_name.*


class NotificationsFragment : BaseFragment(), NotificationItemsAdapter.Listener {

    private val viewModel by viewModels<NotificationsViewModel> { NotificationsModule.Factory() }

    private lateinit var notificationItemsAdapter: NotificationItemsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        buttonAndroidSettings.setOnSingleClickListener {
            viewModel.openSettings()
        }

        deactivateAll.setOnSingleClickListener {
            viewModel.deactivateAll()
        }

        notificationItemsAdapter = NotificationItemsAdapter(this)
        notifications.adapter = notificationItemsAdapter

        switchNotification.setOnClickListener { switchNotification.switchToggle() }

        switchNotification.setOnCheckedChangeListener {
            viewModel.switchAlertNotification(it)
        }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onItemClick(item: NotificationViewItem) {
        viewModel.onDropdownTap(item)
    }

    private fun observeViewModel() {
        viewModel.itemsLiveData.observe(viewLifecycleOwner, Observer {
            notificationItemsAdapter.items = it
            notificationItemsAdapter.notifyDataSetChanged()
        })

        viewModel.openNotificationSettings.observe(viewLifecycleOwner, Observer {
            context?.let {
                val intent = Intent()
                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                intent.putExtra("android.provider.extra.APP_PACKAGE", it.packageName)
                startActivity(intent)
            }
        })

        viewModel.controlsVisible.observe(viewLifecycleOwner, Observer {
            notifications.isVisible = it
            deactivateAll.isVisible = it
        })

        viewModel.setWarningVisible.observe(viewLifecycleOwner, Observer { showWarning ->
            switchNotification.isVisible = !showWarning
            textDescription.isVisible = !showWarning

            textWarning.isVisible = showWarning
            buttonAndroidSettings.isVisible = showWarning
        })

        viewModel.notificationIsOnLiveData.observe(viewLifecycleOwner, Observer { enabled ->
            switchNotification.setChecked(enabled)
        })

        viewModel.openOptionsDialog.observe(viewLifecycleOwner, Observer { (coinName, coinId, mode) ->
            BottomNotificationMenu.show(childFragmentManager, mode, coinName, coinId)
        })

        viewModel.setDeactivateButtonEnabled.observe(viewLifecycleOwner, Observer { enabled ->
            context?.let {
                val color = if (enabled) R.color.lucian else R.color.grey_50
                deactivateAllText.setTextColor(it.getColor(color))
            }

            deactivateAll.isEnabled = enabled
        })

    }
}

class NotificationItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<NotificationViewItem>()

    interface Listener {
        fun onItemClick(item: NotificationViewItem)
    }

    private val coinName = 1
    private val notificationOption = 2

    override fun getItemViewType(position: Int): Int {
        return when (items[position].type) {
            NotificationViewItemType.CoinName -> coinName
            else -> notificationOption
        }
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            notificationOption -> NotificationItemViewHolder(inflate(parent, R.layout.view_holder_setting_with_dropdown, false), onClick = { index -> listener.onItemClick(items[index]) })
            coinName -> NotificationCoinNameViewHolder(inflate(parent, R.layout.view_holder_notification_coin_name, false))
            else -> throw Exception("Invalid view type")
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is NotificationItemViewHolder -> holder.bind(items[position])
            is NotificationCoinNameViewHolder -> holder.bind(items[position])
        }
    }
}

class NotificationCoinNameViewHolder(override val containerView: View)
    : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: NotificationViewItem) {
        coinName.text = item.coinName
    }
}

class NotificationItemViewHolder(override val containerView: View, val onClick: (position: Int) -> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    private val dropdownView = containerView.findViewById<SettingsViewDropdown>(R.id.dropdownView)

    fun bind(item: NotificationViewItem) {
        dropdownView.apply {
            showTitle(item.titleRes?.let { containerView.context.getString(it) })
            showDropdownValue(itemView.context.getString(item.dropdownValue))
            setListPosition(getListPosition(item.type))
        }

        containerView.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
    }

    private fun getListPosition(type: NotificationViewItemType): ListPosition {
        return when (type) {
            NotificationViewItemType.ChangeOption -> ListPosition.First
            else -> ListPosition.Last
        }
    }
}
