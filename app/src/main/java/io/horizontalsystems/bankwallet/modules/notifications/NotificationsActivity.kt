package io.horizontalsystems.bankwallet.modules.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.bankwallet.ui.helpers.AppLayoutHelper
import io.horizontalsystems.views.SettingsViewDropdown
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

        view.showStateSelectorLiveEvent.observe(this, Observer { (itemPosition, selectedPriceAlert) ->
            val priceAlertValues = PriceAlert.State.values()
            val selectorItems = priceAlertValues.map { state ->
                val caption = state.value?.let { "$it%" }
                        ?: getString(R.string.SettingsNotifications_Off)
                SelectorItem(caption, state == selectedPriceAlert.state)
            }
            SelectorDialog
                    .newInstance(selectorItems, null, { position ->
                        presenter.didSelectState(itemPosition, priceAlertValues[position])
                    }, false)
                    .show(supportFragmentManager, "price_alert_value_selector")
        })

        view.notificationIsOn.observe(this, Observer { enabled ->
            switchNotification.showSwitch(enabled, CompoundButton.OnCheckedChangeListener { _, isChecked ->
                presenter.didSwitchAlertNotification(isChecked)
            })

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
        val settingsView = SettingsViewDropdown(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return NotificationItemViewHolder(settingsView, presenter)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NotificationItemViewHolder, position: Int) {
        holder.bind(items[position], position == itemCount - 1, clickable)
    }
}

class NotificationItemViewHolder(override val containerView: SettingsViewDropdown, private val presenter: NotificationsPresenter) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(coinViewItem: NotificationsModule.PriceAlertViewItem, lastElement: Boolean, clickable: Boolean) {
        containerView.showIcon(AppLayoutHelper.getCoinDrawable(containerView.context, coinViewItem.coin.code, coinViewItem.coin.type))
        containerView.showTitle(coinViewItem.title)
        containerView.showSubtitle(coinViewItem.coin.code)
        containerView.showDropdownValue(coinViewItem.state.value?.let { "$it%" } ?: itemView.context.getString(R.string.SettingsNotifications_Off))
        containerView.showBottomBorder(lastElement)
        containerView.isEnabled = clickable

        containerView.setOnClickListener {
            presenter.didTapItem(adapterPosition)
        }
    }
}
