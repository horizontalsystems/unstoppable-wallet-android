package io.horizontalsystems.bankwallet.modules.settings.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.BuildConfig
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.fragment_contact.toolbar

class AboutFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val viewModel by viewModels<AboutViewModel> { AboutModule.Factory() }

        githubButton.setOnSingleClickListener {
            viewModel.onGithubLinkTap()
        }
        siteButton.setOnSingleClickListener {
            viewModel.onSiteLinkTap()
        }
        contactItem.setOnSingleClickListener {
            findNavController().navigate(R.id.aboutAppFragment_to_contactFragment, null, navOptions())
        }
        appStatusItem.setOnSingleClickListener {
            findNavController().navigate(R.id.aboutAppFragment_to_appStatusFragment, null, navOptions())
        }
        termsItem.setOnSingleClickListener {
            findNavController().navigate(R.id.aboutAppFragment_to_termsFragment, null, navOptions())
        }
        rateUsItem.setOnSingleClickListener {
            openRateUs()
        }
        tellFriendsItem.setOnSingleClickListener {
            viewModel.onTellFriendsTap()
        }

        observe(viewModel)

    }

    private fun observe(viewModel: AboutViewModel) {
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
            termsItem.showAttention(!termsAccepted)
        })

        viewModel.appVersionLiveData.observe(viewLifecycleOwner, Observer { version ->
            version?.let {
                var appVersion = getString(R.string.Settings_InfoTitleWithVersion, it)
                if (getString(R.string.is_release) == "false") {
                    appVersion = "$appVersion (${BuildConfig.VERSION_CODE})"
                }
                versionName.text = appVersion
            }
        })
    }

    private fun openRateUs() {
        context?.let { context ->
            val uri = Uri.parse("market://details?id=io.horizontalsystems.bankwallet")  //context.packageName
            val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri)

            goToMarketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)

            try {
                ContextCompat.startActivity(context, goToMarketIntent, null)
            } catch (e: ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=io.horizontalsystems.bankwallet"))
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

}
