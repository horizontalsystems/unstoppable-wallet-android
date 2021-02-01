package io.horizontalsystems.bankwallet.modules.settings.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsAdapter
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsMenuItem
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.inflate
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.view_holder_about_app_header.*

class AboutFragment : BaseFragment(), AboutAppHeaderAdapter.Listener {

    val viewModel by viewModels<AboutViewModel> { AboutModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val contactItem = SettingsMenuItem(R.string.SettingsContact_Title, R.drawable.ic_email, listPosition = ListPosition.First) {
            sendEmail(viewModel.reportEmail)
        }
        val appStatusItem = SettingsMenuItem(R.string.Settings_AppStatus, R.drawable.ic_app_status, listPosition = ListPosition.Middle) {
            findNavController().navigate(R.id.aboutAppFragment_to_appStatusFragment, null, navOptions())
        }
        val termsItem = SettingsMenuItem(R.string.Settings_Terms, R.drawable.ic_terms_20, listPosition = ListPosition.Middle) {
            findNavController().navigate(R.id.aboutAppFragment_to_termsFragment, null, navOptions())
        }
        val rateUsItem = SettingsMenuItem(R.string.Settings_RateUs, R.drawable.ic_star_20, listPosition = ListPosition.Middle) {
            viewModel.onRateUsClicked()
        }
        val shareAppItem = SettingsMenuItem(R.string.Settings_ShareThisWallet, R.drawable.ic_share_20, listPosition = ListPosition.Last) {
            viewModel.onTellFriendsTap()
        }

        val menuItemsAdapter = MainSettingsAdapter(listOf(
                contactItem,
                appStatusItem,
                termsItem,
                rateUsItem,
                shareAppItem
        ))

        val headerAdapter = AboutAppHeaderAdapter(this)

        aboutRecyclerview.adapter = ConcatAdapter(headerAdapter, menuItemsAdapter)

        //observe LiveData

        viewModel.openLinkLiveData.observe(viewLifecycleOwner, Observer { link ->
            val uri = Uri.parse(link)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            activity?.startActivity(intent)
        })

        viewModel.showShareAppLiveData.observe(viewLifecycleOwner, Observer { appWebPageLink ->
            val shareMessage = getString(R.string.SettingsShare_Text) + "\n" + appWebPageLink + "\n"
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.SettingsShare_Title)))
        })

        viewModel.termsAcceptedData.observe(viewLifecycleOwner, Observer { termsAccepted ->
            termsItem.attention = !termsAccepted
            menuItemsAdapter.notifyChanged(termsItem)
        })

        viewModel.showCopiedLiveEvent.observe(viewLifecycleOwner, Observer {
            activity?.let {
                HudHelper.showSuccessMessage(it.findViewById(android.R.id.content), R.string.Hud_Text_EmailAddressCopied)
            }
        })

        headerAdapter.setVersionText(getAppVersion())
    }

    //AboutAppHeaderAdapter.Listener

    override fun onGithubLinkClick() {
        viewModel.onGithubLinkTap()
    }

    override fun onSiteLinkClick() {
        viewModel.onSiteLinkTap()
    }

    private fun getAppVersion(): String {
        var appVersion = getString(R.string.Settings_InfoTitleWithVersion, viewModel.appVersion)
        if (getString(R.string.is_release) == "false") {
            appVersion = "$appVersion (${BuildConfig.VERSION_CODE})"
        }
        return appVersion
    }

    private fun sendEmail(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.didFailSendMail()
        }
    }

}


class AboutAppHeaderAdapter(private val listener: Listener) : RecyclerView.Adapter<AboutAppHeaderAdapter.HeaderViewHolder>() {

    interface Listener {
        fun onGithubLinkClick()
        fun onSiteLinkClick()
    }

    private var versionText = ""

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        return HeaderViewHolder.create(parent, listener)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.bind(versionText)
    }

    fun setVersionText(appVersion: String) {
        versionText = appVersion
        notifyDataSetChanged()
    }

    class HeaderViewHolder(override val containerView: View, private val listener: Listener) : RecyclerView.ViewHolder(containerView), LayoutContainer {

        init {
            githubButton.setOnSingleClickListener { listener.onGithubLinkClick() }
            siteButton.setOnSingleClickListener { listener.onSiteLinkClick() }
        }

        fun bind(version: String) {
            versionNameText.text = version
        }

        companion object {
            const val layout = R.layout.view_holder_about_app_header

            fun create(parent: ViewGroup, listener: Listener) = HeaderViewHolder(inflate(parent, layout, false), listener)
        }

    }
}
