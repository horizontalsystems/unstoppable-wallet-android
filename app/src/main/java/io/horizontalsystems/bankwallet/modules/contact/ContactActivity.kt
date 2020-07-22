package io.horizontalsystems.bankwallet.modules.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.contact.appstatus.AppStatusModule
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.TopMenuItem
import kotlinx.android.synthetic.main.activity_contact.*

class ContactActivity : BaseActivity() {

    lateinit var presenter: ContactPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        presenter = ViewModelProvider(this, ContactModule.Factory()).get(ContactPresenter::class.java)
        val presenterView = presenter.view as ContactView
        val router = presenter.router as ContactRouter

        presenterView.emailLiveData.observe(this, Observer {
            mail.showSubtitle(it)
        })

        presenterView.walletHelpTelegramGroupLiveData.observe(this, Observer {
            walletHelpTelegramGroup.showSubtitle(it)
        })

        presenterView.showCopiedLiveEvent.observe(this, Observer {
            HudHelper.showSuccessMessage(findViewById(android.R.id.content), R.string.Hud_Text_Copied)
        })

        router.sendEmailLiveEvent.observe(this, Observer {
            sendEmail(it)
        })

        router.openTelegramGroupEvent.observe(this, Observer {
            openTelegramGroup(it)
        })

        shadowlessToolbar.bind(
                title = getString(R.string.SettingsContact_Title),
                leftBtnItem = TopMenuItem(R.drawable.ic_back) { onBackPressed() }
        )

        mail.setOnSingleClickListener {
            presenter.didTapEmail()
        }

        walletHelpTelegramGroup.setOnSingleClickListener {
            presenter.didTapWalletHelpTelegram()
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
