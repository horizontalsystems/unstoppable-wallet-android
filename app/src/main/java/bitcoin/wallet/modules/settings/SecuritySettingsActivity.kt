package bitcoin.wallet.modules.settings

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.security.SecurityUtils
import bitcoin.wallet.lib.AlertDialogFragment
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.pin.PinModule
import bitcoin.wallet.modules.restore.RestoreModule
import bitcoin.wallet.ui.dialogs.BottomConfirmAlert
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : BaseActivity(), BottomConfirmAlert.Listener {

    private lateinit var viewModel: SecuritySettingsViewModel

    enum class Action {
        OPEN_RESTORE,
        CLEAR_WALLETS
    }

    private var selectedAction: Action? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(SecuritySettingsViewModel::class.java)
        viewModel.init()

        setContentView(R.layout.activity_settings_security)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.settings_security_center)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)

        changePin.setOnClickListener {
            PinModule.startForEditPinAuth(this)
        }

        backupWallet.apply {
            setOnClickListener {
                BackupModule.start(this@SecuritySettingsActivity, BackupPresenter.DismissMode.DISMISS_SELF)
            }
        }

        //fingerprint
        val phoneHasFingerprintSensor = SecurityUtils.phoneHasFingerprintSensor(this)

        fingerprint.visibility = if (phoneHasFingerprintSensor) View.VISIBLE else View.GONE

        if (phoneHasFingerprintSensor) {
            fingerprint.apply {
                switchIsChecked = App.localStorage.isBiometricOn
                setOnClickListener {
                    if (App.localStorage.isBiometricOn || fingerprintCanBeEnabled()) {
                        switchToggle()
                    }
                }

                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    App.localStorage.isBiometricOn = isChecked
                }
            }
        }

        viewModel.wordListBackedUp.observe(this, Observer { wordListBackedUp ->
            wordListBackedUp?.let {
                backupWallet.badge = getInfoBadge(it)
            }
        })

        importWallet.setOnClickListener {
            selectedAction = Action.OPEN_RESTORE
            val confirmationList = mutableListOf(
                    R.string.settings_security_import_wallet_confirmation_1,
                    R.string.settings_security_import_wallet_confirmation_2
            )
            BottomConfirmAlert.show(this, confirmationList, this)
        }

        unlink.setOnClickListener {
            selectedAction = Action.CLEAR_WALLETS
            val confirmationList = mutableListOf(
                    R.string.settings_security_import_wallet_confirmation_1,
                    R.string.settings_security_import_wallet_confirmation_2
            )
            BottomConfirmAlert.show(this, confirmationList, this)
        }

    }

    override fun confirmationSuccess() {
        when(selectedAction) {
            Action.OPEN_RESTORE -> RestoreModule.start(this)
            Action.CLEAR_WALLETS -> {
                App.adapterManager.clear()
                App.localStorage.clearAll()

                val intent = Intent(this, LauncherActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    private fun getInfoBadge(wordListBackedUp: Boolean): Drawable? {
        var infoBadge: Drawable? = null
        if (!wordListBackedUp) {
            infoBadge = resources.getDrawable(R.drawable.info, null)
            infoBadge?.setTint(resources.getColor(R.color.red_warning, null))
        }
        return infoBadge
    }

    private fun fingerprintCanBeEnabled(): Boolean {
        val touchSensorCanBeUsed = SecurityUtils.touchSensorCanBeUsed(this)
        if (!touchSensorCanBeUsed) {
            AlertDialogFragment.newInstance(R.string.settings_error_fingerprint_not_enabled, R.string.settings_error_no_fingerprint_added_yet, R.string.alert_ok)
                    .show(this.supportFragmentManager, "fingerprint_not_enabled_alert")
            return false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
