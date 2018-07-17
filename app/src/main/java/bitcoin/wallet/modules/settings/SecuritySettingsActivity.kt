package bitcoin.wallet.modules.settings

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.backup.BackupModule
import bitcoin.wallet.modules.backup.BackupPresenter
import bitcoin.wallet.modules.main.UnlockActivity
import io.realm.SyncUser
import kotlinx.android.synthetic.main.activity_settings_security.*

class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings_security)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Security Center"

        pinTouchId.apply {
            checkMarkIsShown = true
            setOnClickListener { Log.e("AAA", "pin / touchid clicked! ") }
        }

        backup.apply {
            checkMarkIsShown = true
            setOnClickListener {
                BackupModule.start(this@SecuritySettingsActivity, BackupPresenter.DismissMode.DISMISS_SELF)
            }
        }

        paperKey.setOnClickListener {
            Log.e("AAA", "paper key clicked!")
        }

        removeWallet.titleTextColor = R.color.red_warning
        removeWallet.setOnClickListener {
            Factory.preferencesManager.saveWords(listOf())

            SyncUser.all().forEach { t, user -> user.logOut() }

            val intent = Intent(this, LauncherActivity::class.java)
            startActivity(intent)

            finish()
        }

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
