package io.horizontalsystems.bankwallet.modules.reportproblem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.reportproblem.appstatus.AppStatusModule
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_report_problem.*

class ReportProblemActivity : BaseActivity() {

    lateinit var presenter: ReportProblemPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_problem)

        presenter = ViewModelProviders.of(this, ReportProblemModule.Factory()).get(ReportProblemPresenter::class.java)
        val presenterView = presenter.view as ReportProblemView
        val router = presenter.router as ReportProblemRouter

        presenterView.emailLiveData.observe(this, Observer {
            mail.subtitle = it
        })

        presenterView.telegramGroupLiveData.observe(this, Observer {
            telegram.subtitle = it
        })

        presenterView.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        })

        router.sendEmailLiveEvent.observe(this, Observer {
            sendEmail(it)
        })

        router.openTelegramGroupEvent.observe(this, Observer {
            openTelegramGroup(it)
        })

        router.openAppStatusLiveEvent.observe(this, Observer {
            AppStatusModule.start(this)
        })

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsReport_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPressed() }
        )

        mail.setOnSingleClickListener {
            presenter.didTapEmail()
        }

        telegram.setOnSingleClickListener {
            presenter.didTapTelegram()
        }

        appStatus.setOnSingleClickListener {
            presenter.didTapAppStatus()
        }

        presenter.viewDidLoad()
    }

    private fun openTelegramGroup(group: String) {
        val tgIntent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$group"))
        if (tgIntent.resolveActivity(packageManager) != null) {
            startActivity(tgIntent)
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$group"))
            startActivity(intent)
        }
    }

    private fun sendEmail(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            presenter.didFailSendMail()
        }
    }

}
