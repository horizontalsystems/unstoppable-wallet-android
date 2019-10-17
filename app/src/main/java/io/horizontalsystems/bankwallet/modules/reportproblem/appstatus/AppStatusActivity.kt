package io.horizontalsystems.bankwallet.modules.reportproblem.appstatus

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_app_status.*
import java.util.*


class AppStatusActivity : BaseActivity() {

    private lateinit var presenter: AppStatusPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_status)

        presenter = ViewModelProviders.of(this, AppStatusModule.Factory()).get(AppStatusPresenter::class.java)

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsReport_AppStatus),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() },
                rightBtnItem = TopMenuItem(text = R.string.Alert_Copy) { presenter.didTapCopy(textAppStatus.text.toString()) }
        )

        observeView(presenter.view as AppStatusView)

        presenter.viewDidLoad()
    }

    private fun observeView(view: AppStatusView) {
        view.appStatusLiveData.observe(this, Observer { appStatusMap ->
            textAppStatus.text = formatMapToString(appStatusMap)
        })

        view.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })
    }

    private fun formatMapToString(status: Map<String, Any>?, indentation: String = "", bullet: String = "", level: Int = 0): String? {
        if (status == null)
            return null

        val sb = StringBuilder()
        status.toList().forEach { (key, value) ->
            val title = "$indentation$bullet$key"
            when (value) {
                is Date -> {
                    val date = DateHelper.formatDateInUTC(value.time / 1000, "MMM d, yyyy, HH:mm")
                    sb.appendln("$title: $date")
                }
                is Map<*, *> -> {
                    val formattedValue = formatMapToString(value as? Map<String, Any>, "\t\t$indentation", " - ", level + 1)
                    sb.append("$title:\n$formattedValue${if (level < 2) "\n" else ""}")
                }
                else -> {
                    sb.appendln("$title: $value")
                }
            }
        }

        val statusString = sb.trimEnd()

        return if (statusString.isEmpty()) "" else "$statusString\n"
    }

}
