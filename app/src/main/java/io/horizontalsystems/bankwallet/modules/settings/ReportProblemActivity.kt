package io.horizontalsystems.bankwallet.modules.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import kotlinx.android.synthetic.main.activity_about_settings.*

class ReportProblemActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_problem)

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsReport_Title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() }
        )
    }

    companion object {
        fun start(context: Activity) {
            val intent = Intent(context, ReportProblemActivity::class.java)
            context.startActivity(intent)
        }
    }

}
