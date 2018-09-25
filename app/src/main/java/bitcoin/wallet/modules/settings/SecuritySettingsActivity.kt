package bitcoin.wallet.modules.settings

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.SecurityUtils
import bitcoin.wallet.lib.AlertDialogFragment
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.pin.PinModule
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_security)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_security_center)

        changePin.setOnClickListener {
            PinModule.startForEditPinAuth(this)
        }

        backupWallet.apply {
            setOnClickListener {
                BackupModule.start(this@SecuritySettingsActivity, BackupPresenter.DismissMode.DISMISS_SELF)
            }
        }

        setBackupWalletBadgeAndSubscribe()

        //fingerprint
        val phoneHasFingerprintSensor = SecurityUtils.phoneHasFingerprintSensor(this)

        fingerprint.visibility = if (phoneHasFingerprintSensor) View.VISIBLE else View.GONE

        if (phoneHasFingerprintSensor) {
            fingerprint.apply {
                switchIsChecked = Factory.preferencesManager.isFingerprintEnabled()
                setOnClickListener {
                    if (Factory.preferencesManager.isFingerprintEnabled() || fingerprintCanBeEnabled()) {
                        switchToggle()
                    }
                }

                switchOnCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
                    Factory.preferencesManager.setFingerprintEnabled(isChecked)
                }
            }
        }

    }

    private fun setBackupWalletBadgeAndSubscribe() {
        backupWallet.badge = getInfoBadge(Factory.wordsManager.wordListBackedUp)

        Factory.wordsManager.wordListBackedUpSubject.subscribe {
            backupWallet.badge = getInfoBadge(it)
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
