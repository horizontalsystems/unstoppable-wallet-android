package io.horizontalsystems.bankwallet.modules.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.modules.base.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_about_settings.*

class AboutSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_settings)

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsAbout_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back, onClick = { onBackPressed() })
        )
    }

    companion object {
        fun start(context: Activity) {
            val intent = Intent(context, AboutSettingsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
