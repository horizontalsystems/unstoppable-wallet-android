package io.horizontalsystems.bankwallet.modules.settings.about

import android.content.ActivityNotFoundException
import android.content.Context
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.RateAppManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.settings.main.HsSettingCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

class AboutFragment : BaseFragment() {

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
                    AboutScreen(findNavController())
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(navController: NavController) {
    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                TranslatableString.ResString(R.string.SettingsAboutApp_Title),
                navigationIcon = {
                    HsIconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            tint = ComposeAppTheme.colors.jacob,
                            contentDescription = null,
                        )
                    }
                }
            )

            AboutContent(navController)
        }
    }
}

@Composable
fun AboutContent(
    navController: NavController,
    aboutViewModel: AboutViewModel = viewModel(factory = AboutModule.Factory()),
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))
        AboutHeader(aboutViewModel.appVersion)
        Spacer(Modifier.height(24.dp))
        InfoTextBody(text = stringResource(R.string.SettingsTerms_Text))
        Spacer(Modifier.height(24.dp))
        SettingSections(aboutViewModel, navController)
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun SettingSections(viewModel: AboutViewModel, navController: NavController) {

    val context = LocalContext.current
    val termsShowAlert = viewModel.termsShowAlert

    CellSingleLineLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.SettingsAboutApp_WhatsNew,
                R.drawable.ic_info_20,
                onClick = {
                    navController.slideFromRight(R.id.releaseNotesFragment)
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))

    CellSingleLineLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_AppStatus,
                R.drawable.ic_app_status,
                onClick = {
                    navController.slideFromRight(R.id.appStatusFragment)
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Terms,
                R.drawable.ic_terms_20,
                showAlert = termsShowAlert,
                onClick = {
                    navController.slideFromBottom(R.id.termsFragment)
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Privacy,
                R.drawable.ic_user_20,
                onClick = {
                    navController.slideFromRight(R.id.privacyFragment)
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellSingleLineLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.SettingsAboutApp_Github,
                R.drawable.ic_github_20,
                onClick = { LinkHelper.openLinkInAppBrowser(context, viewModel.githubLink) }
            )
        }, {
            HsSettingCell(
                R.string.SettingsAboutApp_Twitter,
                R.drawable.ic_twitter_20,
                onClick = { LinkHelper.openLinkInAppBrowser(context, viewModel.twitterLink) }
            )
        }, {
            HsSettingCell(
                R.string.SettingsAboutApp_Site,
                R.drawable.ic_globe,
                onClick = { LinkHelper.openLinkInAppBrowser(context, viewModel.appWebPageLink) }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellSingleLineLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_RateUs,
                R.drawable.ic_star_20,
                onClick = { RateAppManager.openPlayMarket(context) }
            )
        }, {
            HsSettingCell(
                R.string.Settings_ShareThisWallet,
                R.drawable.ic_share_20,
                onClick = { shareAppLink(viewModel.appWebPageLink, context) }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellSingleLineLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.SettingsContact_Title,
                R.drawable.ic_email,
                onClick = { sendEmail(viewModel.reportEmail, context) }
            )
        }
    )
}

@Composable
fun AboutHeader(appVersion: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
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
            subhead2_grey(
                text = stringResource(R.string.Settings_InfoTitleWithVersion, appVersion),
                maxLines = 1,
            )
        }
    }
}

private fun shareAppLink(appLink: String, context: Context) {
    val shareMessage = Translator.getString(R.string.SettingsShare_Text) + "\n" + appLink + "\n"
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            Translator.getString(R.string.SettingsShare_Title)
        )
    )
}

private fun sendEmail(recipient: String, context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        TextHelper.copyText(recipient)
    }
}

@Preview
@Composable
private fun previewAboutScreen() {
    ComposeAppTheme {
        Column{
            AboutHeader("0.24")
            Spacer(Modifier.height(32.dp))
            CellSingleLineLawrenceSection(
                listOf({
                    HsSettingCell(
                        R.string.Settings_RateUs,
                        R.drawable.ic_star_20,
                        showAlert = true,
                        onClick = {  }
                    )
                }, {
                    HsSettingCell(
                        R.string.Settings_ShareThisWallet,
                        R.drawable.ic_share_20,
                        onClick = {  }
                    )
                })
            )
        }
    }
}
