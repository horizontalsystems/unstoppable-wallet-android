package bitcoin.wallet.modules.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import kotlinx.android.synthetic.main.activity_about_settings.*

class AboutSettingsActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.back)
        supportActionBar?.title = getString(R.string.settings_about_title)
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

    companion object {
        fun start(context: Activity) {
            val intent = Intent(context, AboutSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
