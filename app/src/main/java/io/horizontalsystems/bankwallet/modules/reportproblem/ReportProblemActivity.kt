package io.horizontalsystems.bankwallet.modules.reportproblem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.ui.extensions.TopMenuItem
import io.horizontalsystems.bankwallet.viewHelpers.HudHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import kotlinx.android.synthetic.main.activity_about_settings.shadowlessToolbar
import kotlinx.android.synthetic.main.activity_report_problem.*

class ReportProblemActivity : BaseActivity() {

    lateinit var presenter: ReportProblemPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_problem)

        presenter = ViewModelProviders.of(this, ReportProblemModule.Factory()).get(ReportProblemPresenter::class.java)

        val router = presenter.router as ReportProblemRouter
        router.sendEmailLiveEvent.observe(this, Observer {
            composeEmailOrCopyToClipboard(it)
        })

        router.openTelegramGroupEvent.observe(this, Observer {
            openTelegramGroup(it)
        })

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsReport_Title),
                leftBtnItem = TopMenuItem(R.drawable.back) { onBackPressed() }
        )

        mail.setSubtitle(presenter.email)
        mail.setOnSingleClickListener {
            presenter.didTapEmail()
        }

        telegram.setSubtitle(presenter.telegramGroup)
        telegram.setOnSingleClickListener {
            presenter.didTapTelegram()
        }
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

    private fun composeEmailOrCopyToClipboard(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            TextHelper.copyText(recipient)
            HudHelper.showSuccessMessage(R.string.Hud_Text_Copied, 500)
        }
    }

}
