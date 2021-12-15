package io.horizontalsystems.bankwallet.modules.settings.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.modules.settings.main.AppSetting
import io.horizontalsystems.bankwallet.modules.settings.main.SettingViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

class AboutFragment : BaseFragment() {

    val viewModel by viewModels<AboutViewModel> { AboutModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    AboutScreen(
                        viewModel,
                        onClickNavigation = { findNavController().popBackStack() },
                        { setting -> onSettingClick(setting) },
                    )
                }
            }
        }
    }

    private fun onSettingClick(setting: AppSetting) {
        when (setting) {
            AppSetting.Github -> openLink(viewModel.githubLink)
            AppSetting.AppWebsite -> openLink(viewModel.appWebPageLink)
            AppSetting.Contact -> sendEmail(viewModel.reportEmail)
            AppSetting.TellFriends -> shareAppLink(viewModel.appWebPageLink)
            AppSetting.RateUs -> rateUs()
            else -> openPage(setting)
        }
    }

    private fun openPage(setting: AppSetting) {
        setting.destination?.let {
            findNavController().navigate(it, setting.navigationBundle, navOptions())
        }
    }

    private fun rateUs() {
        context?.let { RateAppManager.openPlayMarket(it) }
    }

    private fun openLink(link: String) {
        context?.let { LinkHelper.openLinkInAppBrowser(it, link) }
    }

    private fun shareAppLink(appLink: String) {
        val shareMessage = getString(R.string.SettingsShare_Text) + "\n" + appLink + "\n"
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
        startActivity(
            Intent.createChooser(
                shareIntent,
                getString(R.string.SettingsShare_Title)
            )
        )
    }

    private fun sendEmail(recipient: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        }

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            TextHelper.copyText(recipient)

            activity?.let {
                HudHelper.showSuccessMessage(
                    it.findViewById(android.R.id.content),
                    R.string.Hud_Text_EmailAddressCopied
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(
    viewModel: AboutViewModel,
    onClickNavigation: () -> Unit,
    onSettingClick: (AppSetting) -> Unit,
) {

    val settingItems by viewModel.settingItemsLiveData.observeAsState()

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.SettingsAboutApp_Title),
                navigationIcon = {
                    IconButton(onClick = onClickNavigation) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            )

            settingItems?.let {
                AboutContent(it, viewModel.appVersion, onSettingClick,)
            }
        }
    }
}

@Composable
fun AboutContent(
    sections: List<List<SettingViewItem>>,
    appVersion: String,
    onSettingClick: (AppSetting) -> Unit,
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        AboutHeader(appVersion)
//        SettingSections(sections, onSettingClick)
    }
}

@Composable
fun AboutHeader(appVersion: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 26.dp, top = 24.dp, end = 16.dp, bottom = 32.dp)
    ) {
        Image(
            modifier = Modifier.size(72.dp),
            painter = painterResource(id = R.drawable.ic_app_logo_72),
            contentDescription = null,
        )
        Column(
            Modifier.height(72.dp).padding(start = 16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.App_Name),
                style = ComposeAppTheme.typography.headline1,
                color = ComposeAppTheme.colors.leah,
                maxLines = 1,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion),
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun previewAboutScreen() {
    val section1 = listOf(
        SettingViewItem(AppSetting.WhatsNew),
    )
    val section2 = listOf(
        SettingViewItem(AppSetting.TellFriends),
        SettingViewItem(AppSetting.Terms, showAlert = true),
    )

    val testItems = listOf(section1, section2)

    ComposeAppTheme {
        AboutContent(testItems, "0.24", { })
    }
}
