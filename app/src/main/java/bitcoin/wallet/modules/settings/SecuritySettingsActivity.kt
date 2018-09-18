package bitcoin.wallet.modules.settings

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.SecurityUtils
import bitcoin.wallet.lib.AlertDialogFragment
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.main.UnlockActivity
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lightMode = Factory.preferencesManager.isLightModeEnabled
        setTheme(if (lightMode) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)

        setContentView(R.layout.activity_settings_security)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_security_center)

        changePin.setOnClickListener {
            Log.e("SecuritySettingsAct", "change pin clicked")
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
                switchIsChecked = Factory.preferencesManager.isFingerprintEnabled
                setOnClickListener {
                    if (Factory.preferencesManager.isFingerprintEnabled || fingerprintCanBeEnabled()) {
                        switchToggle()
                    }
                }

                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    Factory.preferencesManager.isFingerprintEnabled = isChecked
                }
            }
        }

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

    override fun onResume() {
        super.onResume()

        if (App.promptPin) {
            startActivity(Intent(this, UnlockActivity::class.java))
            return
        }

        App.promptPin = false
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
