package cash.p.terminal.modules.settings.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.core.composablePopup
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.modules.releasenotes.ReleaseNotesScreen
import cash.p.terminal.modules.settings.appstatus.AppStatusScreen
import cash.p.terminal.modules.settings.main.HsSettingCell
import cash.p.terminal.modules.settings.privacy.PrivacyScreen
import cash.p.terminal.modules.settings.terms.TermsScreen
import cash.p.terminal.ui_compose.components.AppBar
import io.horizontalsystems.core.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

class AboutFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        AboutNavHost(navController)
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
                SettingSections(aboutViewModel, navController)
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun SettingSections(
    viewModel: AboutViewModel,
    navController: NavController
) {

    val context = LocalContext.current
    val termsShowAlert = viewModel.termsShowAlert

    CellUniversalLawrenceSection(
        listOf {
            HsSettingCell(
                title = R.string.SettingsAboutApp_AppVersion,
                icon = R.drawable.ic_info_20,
                value = viewModel.appVersion,
                onClick = {
                    navController.navigate(ReleaseNotesPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.WhatsNew))
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

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.AppStatus))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Terms,
                R.drawable.ic_terms_20,
                showAlert = termsShowAlert,
                onClick = {
                    navController.navigate(TermsPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.Terms))
                }
            )
        }, {
            HsSettingCell(
                R.string.Settings_Privacy,
                R.drawable.ic_user_20,
                onClick = {
                    navController.navigate(PrivacyPage)

                    stat(page = StatPage.AboutApp, event = StatEvent.Open(StatPage.Privacy))
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
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.githubLink)

                    stat(page = StatPage.AboutApp, event= StatEvent.Open(StatPage.ExternalGithub))
                }
            )
        }, {
            HsSettingCell(
                R.string.SettingsAboutApp_Site,
                R.drawable.ic_globe,
                onClick = {
                    LinkHelper.openLinkInAppBrowser(context, viewModel.appWebPageLink)

                    stat(page = StatPage.AboutApp, event= StatEvent.Open(StatPage.ExternalWebsite))
                }
            )
        })
    )
    
    VSpacer(32.dp)
}
