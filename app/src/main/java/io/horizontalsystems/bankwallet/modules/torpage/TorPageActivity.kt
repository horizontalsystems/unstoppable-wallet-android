package io.horizontalsystems.bankwallet.modules.torpage

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.views.AlertDialogFragment
import kotlinx.android.synthetic.main.activity_tor_page.*

class TorPageActivity : CoreActivity() {

    lateinit var presenter: TorPagePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tor_page)
        setSupportActionBar(toolbar)

        presenter = ViewModelProvider(this, TorPageModule.Factory())
                .get(TorPagePresenter::class.java)

        presenter.viewDidLoad()

        observeView(presenter.view as TorPageView)
        observeRouter(presenter.router as TorPageRouter)

        controlView.setOnClickListener {
            switchView.toggle()
        }
    }

    private fun observeRouter(router: TorPageRouter) {
        router.closePage.observe(this, Observer {
            onBackPressed()
        })
    }

    private fun observeView(view: TorPageView) {
        view.setTorSwitch.observe(this, Observer { checked ->
            setSwitch(checked)
        })

        view.setTorConnectionStatus.observe(this, Observer { torStatus ->
            torStatus?.let {
                when (torStatus) {
                    TorStatus.Connecting -> {
                        connectionSpinner.visibility = View.VISIBLE
                        controlIcon.setTint(getTint(R.color.grey))
                        controlIcon.bind(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connecting)
                    }
                    TorStatus.Connected -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor_connected)
                        subtitleText.text = getString(R.string.TorPage_Connected)
                    }
                    TorStatus.Failed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor_status_error)
                        subtitleText.text = getString(R.string.TorPage_Failed)
                    }
                    TorStatus.Closed -> {
                        connectionSpinner.visibility = View.GONE
                        controlIcon.setTint(getTint(R.color.yellow_d))
                        controlIcon.bind(R.drawable.ic_tor)
                        subtitleText.text = getString(R.string.TorPage_ConnectionClosed)
                    }
                }
            }
        })

        view.notificationsNotEnabledAlert.observe(this, Observer {
            showNotificationsNotEnabledAlert()
        })
    }

    private fun setSwitch(checked: Boolean) {
        switchView.setOnCheckedChangeListener(null)
        switchView.isChecked = checked
        switchView.setOnCheckedChangeListener { _, isChecked ->
            presenter.onTorSwitch(isChecked)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.tor_page_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuClose -> {
                presenter.onClose()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showNotificationsNotEnabledAlert() {
        AlertDialogFragment.newInstance(
                descriptionString = getString(R.string.SettingsSecurity_NotificationsDisabledWarning),
                buttonText = R.string.Button_Enable,
                cancelButtonText = R.string.Alert_Cancel,
                cancelable = true,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        openAppNotificationSettings()
                    }

                    override fun onCancel() {
                        setSwitch(false)
                    }
                }).show(supportFragmentManager, "alert_dialog_notification")
    }

    private fun openAppNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("android.provider.extra.APP_PACKAGE", packageName)
        startActivity(intent)
    }

    private fun getTint(color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))
}
