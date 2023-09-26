package io.horizontalsystems.bankwallet.modules.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.releasenotes.ReleaseNotesScreen
import io.horizontalsystems.bankwallet.modules.settings.appstatus.AppStatusScreen
import io.horizontalsystems.bankwallet.modules.settings.main.HsSettingCell
import io.horizontalsystems.bankwallet.modules.settings.privacy.PrivacyScreen
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.InfoTextBody
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController

class AboutFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            AboutNavHost(findNavController())
        }
    }

}

private const val AboutPage = "about"
private const val ReleaseNotesPage = "release_notes"
private const val AppStatusPage = "app_status"
private const val PrivacyPage = "privacy"
private const val TermsPage = "terms"

@Composable
private fun AboutNavHost(fragmentNavController: NavController) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AboutPage,
    ) {
        composable(AboutPage) {
            AboutScreen(
                navController,
                { fragmentNavController.slideFromBottom(R.id.contactOptionsDialog) },
                { fragmentNavController.popBackStack() }
            )
        }
        composablePage(ReleaseNotesPage) {
            ReleaseNotesScreen(false, { navController.popBackStack() })
        }
        composablePage(AppStatusPage) { AppStatusScreen(navController) }
        composablePage(PrivacyPage) { PrivacyScreen(navController) }
        composablePopup(TermsPage) { TermsScreen(navController) }
    }
}

@Composable
private fun AboutScreen(
    navController: NavController,
    showContactOptions: () -> Unit,
    onBackPress: () -> Unit,
    aboutViewModel: AboutViewModel = viewModel(factory = AboutModule.Factory()),
) {
    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                title = stringResource(R.string.SettingsAboutApp_Title),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                }
            )

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                AboutHeader(aboutViewModel.appVersion)
                Spacer(Modifier.height(24.dp))
                InfoTextBody(text = stringResource(R.string.SettingsTerms_Text))
                Spacer(Modifier.height(24.dp))
                SettingSections(aboutViewModel, navController, showContactOptions)
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: AboutViewModel,
    navController: NavController,
    showContactOptions: () -> Unit
) {

    val context = LocalContext.current
    val termsShowAlert = viewModel.termsShowAlert

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                R.string.SettingsAboutApp_WhatsNew,
                R.drawable.ic_info_20,
                onClick = {
                    navController.navigate(ReleaseNotesPage)
                }
            )
        }
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
        listOf({
            HsSettingCell(
                R.string.Settings_AppStatus,
                R.drawable.ic_app_status,
                onClick = {
                    navController.navigate(AppStatusPage)
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Terms,
                R.drawable.ic_terms_20,
                showAlert = termsShowAlert,
                onClick = {
                    navController.navigate(TermsPage)
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Privacy,
                R.drawable.ic_user_20,
                onClick = {
                    navController.navigate(PrivacyPage)
                }
            )
        })
    )

    Spacer(Modifier.height(32.dp))

    CellUniversalLawrenceSection(
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
    
    VSpacer(32.dp)
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

@Preview
@Composable
private fun previewAboutScreen() {
    ComposeAppTheme {
        Column {
            AboutHeader("0.24")
            Spacer(Modifier.height(32.dp))
            CellSingleLineLawrenceSection(
                listOf({
                    HsSettingCell(
                        R.string.Settings_RateUs,
                        R.drawable.ic_star_20,
                        showAlert = true,
                        onClick = { }
                    )
                }, {
                    HsSettingCell(
                        R.string.Settings_ShareThisWallet,
                        R.drawable.ic_share_20,
                        onClick = { }
                    )
                })
            )
        }
    }
}
