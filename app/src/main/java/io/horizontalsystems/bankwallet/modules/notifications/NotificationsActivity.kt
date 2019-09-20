package io.horizontalsystems.bankwallet.modules.notifications

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.components.CellView
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.activity_alerts.*

class NotificationsActivity : BaseActivity() {

    private lateinit var presenter: NotificationsPresenter
    private lateinit var notificationItemsAdapter: NotificationItemsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)
        shadowlessToolbar.bind(
                title = getString(R.string.SettingsNotifications_Title),
                leftBtnItem = TopMenuItem(R.drawable.back, onClick = { onBackPressed() })
        )

        presenter = ViewModelProviders.of(this, NotificationsModule.Factory()).get(NotificationsPresenter::class.java)

        observeView(presenter.view as NotificationsView)

        notificationItemsAdapter = NotificationItemsAdapter(presenter)
        notifications.adapter = notificationItemsAdapter

        presenter.viewDidLoad()
    }

    private fun observeView(view: NotificationsView) {
        view.itemsLiveData.observe(this, Observer {
            notificationItemsAdapter.items = it
            notificationItemsAdapter.notifyDataSetChanged()
        })
    }
}

class NotificationItemsAdapter(private val presenter: NotificationsPresenter) : RecyclerView.Adapter<NotificationItemViewHolder>() {
    var items = listOf<NotificationsModule.PriceAlertViewItem>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationItemViewHolder {
        return NotificationItemViewHolder(CellView(parent.context), presenter)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: NotificationItemViewHolder, position: Int) {
        holder.bind(items[position], position == itemCount - 1)
    }
}

class NotificationItemViewHolder(override val containerView: CellView, private val presenter: NotificationsPresenter) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    private val spinner = containerView.spinner
    private val priceAlertValues = PriceAlert.State.values()

    init {
        val spinnerValues = priceAlertValues.map {
            it.value?.let { "$it%" } ?: itemView.context.getString(R.string.SettingsNotifications_Off)
        }

        spinner.visibility = View.VISIBLE
        spinner.adapter = ArrayAdapter(itemView.context, android.R.layout.simple_spinner_dropdown_item, spinnerValues)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                presenter.didSelectState(adapterPosition, priceAlertValues[position])
            }
        }
    }

    fun bind(coinViewItem: NotificationsModule.PriceAlertViewItem, lastElement: Boolean) {
        containerView.icon = coinViewItem.code
        containerView.title = coinViewItem.title
        containerView.bottomBorder = lastElement

        spinner.setSelection(priceAlertValues.indexOfFirst { it == coinViewItem.state })
    }
}
