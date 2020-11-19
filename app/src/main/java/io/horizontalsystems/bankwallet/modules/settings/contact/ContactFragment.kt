package io.horizontalsystems.bankwallet.modules.settings.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_contact.*

class ContactFragment : BaseFragment() {

    lateinit var presenter: ContactPresenter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contact, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSupportActionBar(toolbar, true)

        presenter = ViewModelProvider(this, ContactModule.Factory()).get(ContactPresenter::class.java)

        val presenterView = presenter.view as ContactView
        val router = presenter.router as ContactRouter

        presenterView.emailLiveData.observe(viewLifecycleOwner, Observer {
            mail.showSubtitle(it)
        })

        presenterView.walletHelpTelegramGroupLiveData.observe(viewLifecycleOwner, Observer {
            walletHelpTelegramGroup.showSubtitle(it)
        })

        presenterView.showCopiedLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_Copied)
            }
        })

        router.sendEmailLiveEvent.observe(viewLifecycleOwner, Observer {
            sendEmail(it)
        })

        router.openTelegramGroupEvent.observe(viewLifecycleOwner, Observer {
            openTelegramGroup(it)
        })

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
        val packageManager = activity?.packageManager
        if (packageManager != null && tgIntent.resolveActivity(packageManager) != null) {
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

        val packageManager = activity?.packageManager
        if (packageManager != null && intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            presenter.didFailSendMail()
        }
    }
}
