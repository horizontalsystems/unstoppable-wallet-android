package io.horizontalsystems.bankwallet.modules.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.views.CellView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_alerts.*

class NotificationsActivity : BaseActivity() {

    private lateinit var presenter: NotificationsPresenter
    private lateinit var notificationItemsAdapter: NotificationItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, NotificationsModule.Factory()).get(NotificationsPresenter::class.java)

        observeView(presenter.view as NotificationsView)
        observeRouter(presenter.router as NotificationsRouter)

        buttonAndroidSettings.setOnSingleClickListener {
            presenter.didClickOpenSettings()
        }

        deactivateAll.setOnSingleClickListener {
            presenter.didClickDeactivateAll()
        }

        notificationItemsAdapter = NotificationItemsAdapter(presenter)
        notifications.adapter = notificationItemsAdapter

        switchNotification.setOnClickListener { switchNotification.switchToggle() }

        presenter.viewDidLoad()
    }

    private fun observeView(view: NotificationsView) {
        view.itemsLiveData.observe(this, Observer {
            notificationItemsAdapter.items = it
            notificationItemsAdapter.notifyDataSetChanged()
        })

        view.toggleWarningLiveData.observe(this, Observer { showWarning ->
            if (showWarning) {
                switchNotification.visibility = View.GONE
                textDescription.visibility = View.GONE
                notifications.visibility = View.GONE
                deactivateAll.visibility = View.GONE

                textWarning.visibility = View.VISIBLE
                buttonAndroidSettings.visibility = View.VISIBLE
            } else {
                switchNotification.visibility = View.VISIBLE
                textDescription.visibility = View.VISIBLE
                notifications.visibility = View.VISIBLE
                deactivateAll.visibility = View.VISIBLE

                textWarning.visibility = View.GONE
                buttonAndroidSettings.visibility = View.GONE
            }
        })

        view.showStateSelectorLiveEvent.observe(this, Observer { (itemPosition, priceAlert) ->
            val priceAlertValues = PriceAlert.State.values().toList()
            PriceAlertStateSelectorDialog.newInstance(object : PriceAlertStateSelectorDialog.Listener {
                override fun onSelect(position: Int) {
                    presenter.didSelectState(itemPosition, priceAlertValues[position])
                }
            }, priceAlertValues, priceAlert.state)
                    .show(supportFragmentManager, "price_alert_value_selector")
        })

        view.notificationIsOn.observe(this, Observer { enabled ->
            switchNotification.apply {
                switchIsChecked = enabled

                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    presenter.didSwitchAlertNotification(isChecked)
                }
            }

            notifications.alpha = if (enabled) 1f else 0.5f
            deactivateAll.alpha = if (enabled) 1f else 0.5f

            //enable/disable clicks on related elements
            notificationItemsAdapter.clickable = enabled
            notificationItemsAdapter.notifyDataSetChanged()

            deactivateAll.isEnabled = enabled
        })
    }

    private fun observeRouter(router: NotificationsRouter) {
        router.openNotificationSettingsLiveEvent.observe(this, Observer {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

            //for Android 5-7
            intent.putExtra("app_package", packageName)
            intent.putExtra("app_uid", applicationInfo.uid)

            // for Android 8 and above
            intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)

            startActivity(intent)
        })
    }
}

class NotificationItemsAdapter(private val presenter: NotificationsPresenter) : RecyclerView.Adapter<NotificationItemViewHolder>() {
    var items = listOf<NotificationsModule.PriceAlertViewItem>()
    var clickable = true

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationItemViewHolder {
        val cellView = CellView(parent.context)
        cellView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return NotificationItemViewHolder(cellView, presenter)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NotificationItemViewHolder, position: Int) {
        holder.bind(items[position], position == itemCount - 1, clickable)
    }
}

class NotificationItemViewHolder(override val containerView: CellView, private val presenter: NotificationsPresenter) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coinViewItem: NotificationsModule.PriceAlertViewItem, lastElement: Boolean, clickable: Boolean) {
        containerView.coinIcon = coinViewItem.code
        containerView.title = coinViewItem.title
        containerView.subtitle = coinViewItem.code
        containerView.dropDownText = coinViewItem.state.value?.let { "$it%" }
                ?: itemView.context.getString(R.string.SettingsNotifications_Off)
        containerView.dropDownArrow = true
        containerView.bottomBorder = lastElement

        containerView.setOnClickListener {
            presenter.didTapItem(adapterPosition)
        }
        containerView.isEnabled = clickable
    }

}
