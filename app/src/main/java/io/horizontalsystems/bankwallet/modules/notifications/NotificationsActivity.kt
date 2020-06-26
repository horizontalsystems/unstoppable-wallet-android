package io.horizontalsystems.bankwallet.modules.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.views.SettingsViewDropdown
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_alerts.*
import kotlinx.android.synthetic.main.view_holder_notification_coin_name.*

class NotificationsActivity : BaseActivity(), NotificationItemsAdapter.Listener {

    private val viewModel by viewModels<NotificationsViewModel>{ NotificationsModule.Factory() }

    private lateinit var notificationItemsAdapter: NotificationItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        buttonAndroidSettings.setOnSingleClickListener {
            viewModel.openSettings()
        }

        deactivateAll.setOnSingleClickListener {
            viewModel.deactivateAll()
        }

        notificationItemsAdapter = NotificationItemsAdapter(this)
        notifications.adapter = notificationItemsAdapter

        switchNotification.setOnClickListener { switchNotification.switchToggle() }

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
        viewModel.itemsLiveData.observe(this, Observer {
            notificationItemsAdapter.items = it
            notificationItemsAdapter.notifyDataSetChanged()
        })

        viewModel.openNotificationSettings.observe(this, Observer {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
            startActivity(intent)
        })

        viewModel.setWarningVisible.observe(this, Observer { showWarning ->
            notifications.isVisible = !showWarning
            deactivateAll.isVisible = !showWarning
            switchNotification.isVisible = !showWarning
            textDescription.isVisible = !showWarning

            textWarning.isVisible = showWarning
            buttonAndroidSettings.isVisible = showWarning
        })

        viewModel.notificationIsOnLiveData.observe(this, Observer { enabled ->
            switchNotification.showSwitch(enabled, CompoundButton.OnCheckedChangeListener { _, isChecked ->
                viewModel.switchAlertNotification(isChecked)
            })

            notifications.isVisible = enabled
            deactivateAll.isVisible = enabled
        })

        viewModel.openOptionsDialog.observe(this, Observer {(coinName, coinCode, mode) ->
            BottomNotificationMenu.show(supportFragmentManager, mode, coinName, coinCode)
        })
    }

}

class NotificationItemsAdapter(private val listener: Listener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = listOf<NotificationViewItem>()

    interface Listener{
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
        return when(viewType){
            notificationOption -> {
                val settingsView = SettingsViewDropdown(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                NotificationItemViewHolder(settingsView, onClick = { index -> listener.onItemClick(items[index]) })
            }
            coinName -> NotificationCoinNameViewHolder(inflate(parent, R.layout.view_holder_notification_coin_name, false)
            )
            else -> throw Exception("Invalid view type")
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder){
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

class NotificationItemViewHolder(override val containerView: SettingsViewDropdown, val onClick: (position: Int)-> Unit) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: NotificationViewItem) {
        item.titleRes?.let {
            containerView.showTitle(itemView.context.getString(it))
        }
        containerView.showDropdownValue(itemView.context.getString(item.dropdownValue))
        containerView.showBottomBorder(item.type == NotificationViewItemType.TrendOption)

        containerView.setOnClickListener {
            onClick(bindingAdapterPosition)
        }
    }
}
